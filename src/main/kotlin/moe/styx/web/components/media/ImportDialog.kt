package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.data.value.ValueChangeMode
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.data.MediaInfo
import moe.styx.common.extension.readableSize
import moe.styx.common.extension.toBoolean
import moe.styx.common.extension.toInt
import moe.styx.db.tables.ChangesTable
import moe.styx.db.tables.ImageTable
import moe.styx.db.tables.MediaEntryTable
import moe.styx.db.tables.MediaInfoTable
import moe.styx.downloader.parsing.parseEpisodeAndVersion
import moe.styx.downloader.utils.getMediaInfo
import moe.styx.web.data.sendDiscordHookEmbed
import moe.styx.web.dbClient
import moe.styx.web.getURL
import moe.styx.web.newGUID
import moe.styx.web.topNotification
import org.jetbrains.exposed.sql.selectAll
import org.vaadin.filesystemdataprovider.FileTypeResolver
import org.vaadin.filesystemdataprovider.FilesystemData
import org.vaadin.filesystemdataprovider.FilesystemDataProvider
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class ImportDialog(val media: Media) : Dialog() {
    private var selected: Set<File> = emptySet()
    private lateinit var layout: VerticalLayout
    private lateinit var firstEpDatePicker: DatePicker
    private lateinit var notifyDiscord: Checkbox
    private var currentOffset = 0
    private var converted: List<FileEPCombo> = emptyList()
    private lateinit var mainGrid: Grid<FileEPCombo>

    init {
        setSizeFull()
        maxWidth = "800px"
        maxHeight = "600px"
        verticalLayout {
            setSizeFull()
            button("Select Files/Folder") {
                onClick {
                    FileBrowserDialog {
                        selected = it
                        update()
                    }.open()
                }
            }
            horizontalLayout {
                defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                integerField("Episode Offset") {
                    value = 0
                    isStepButtonsVisible = true
                    step = 1
                    width = "200px"
                    valueChangeMode = ValueChangeMode.EAGER
                    addValueChangeListener {
                        currentOffset = it?.value ?: 0
                        update()
                    }
                }
                firstEpDatePicker = datePicker("First episode date") {
                    setTooltipText("Every episode will be +7 days from the last.")
                    value = LocalDate.of(2000, 1, 1)
                    min = LocalDate.of(1990, 1, 1)
                }
                notifyDiscord = checkBox("Notify Discord")
            }
            layout = verticalLayout {
                setSizeFull()
                mainGrid = grid<FileEPCombo> {
                    setSizeFull()
                    columnFor(FileEPCombo::file, converter = { it!!.name }) {
                        setHeader("File")
                        isExpand = true
                    }
                    columnFor(FileEPCombo::episode) {
                        setHeader("Parsed Episode")
                        setWidth("250px")
                    }
                }
            }
            button("Import") {
                onClick {
                    val list = converted.filter { it.episode.isNotBlank() }.sortedBy { it.episode }
                    if (list.isEmpty()) {
                        topNotification("No valid episode numbers found.")
                        return@onClick
                    }
                    val existing = dbClient.transaction { MediaEntryTable.query { selectAll().where { mediaID eq media.GUID }.toList() } }
                    var date = firstEpDatePicker.value
                    var newEntries = 0
                    list.forEach { combo ->
                        val time = date.atTime(16, 0).atZone(ZoneId.systemDefault()).toInstant().epochSecond
                        val existingEntry = existing.find { it.entryNumber.toDoubleOrNull() == combo.episode.toDoubleOrNull() }
                        val entry =
                            existingEntry?.copy(filePath = combo.file.absolutePath, fileSize = combo.file.length(), originalName = combo.file.name)
                                ?: MediaEntry(
                                    newGUID(), media.GUID, time, combo.episode, null, null, null, null, null, combo.file
                                        .absolutePath, combo.file.length(), combo.file.name
                                )
                        if (existingEntry == null)
                            newEntries++
                        dbClient.transaction { MediaEntryTable.upsertItem(entry) }

                        val mediaInfoResult = combo.file.getMediaInfo()
                        if (mediaInfoResult != null) {
                            dbClient.transaction {
                                MediaInfoTable.upsertItem(
                                    MediaInfo(
                                        entry.GUID,
                                        mediaInfoResult.videoCodec(),
                                        mediaInfoResult.videoBitDepth(),
                                        mediaInfoResult.videoResolution(),
                                        mediaInfoResult.hasEnglishDub().toInt(),
                                        mediaInfoResult.hasGermanDub().toInt(),
                                        mediaInfoResult.hasGermanSub().toInt()
                                    )
                                )
                            }
                        }
                        date = date.plusDays(7)
                    }
                    dbClient.transaction { ChangesTable.setToNow(true, true) }
                    val image = media.thumbID?.let {
                        dbClient.transaction {
                            ImageTable.query { selectAll().where { GUID eq it }.toList() }.firstOrNull()
                        }
                    }
                    val thumbnail = image?.getURL()
                    if (notifyDiscord.value && thumbnail != null) {
                        if (media.isSeries.toBoolean()) {
                            sendDiscordHookEmbed("Batch added", "${media.name} ($newEntries EPs)", thumbnail)
                        } else {
                            sendDiscordHookEmbed("Movie added", media.name, thumbnail)
                        }
                    }
                    this@ImportDialog.close()
                }
            }
        }
    }

    private fun update() {
        var list = selected.toList()
        val folder = selected.find { it.isDirectory }
        if (folder != null)
            list = folder.listFiles()?.filter { it.isFile } ?: emptyList()
        if (list.isEmpty()) {
            return
        }
        val entries = list.map {
            val parsed = parseEpisodeAndVersion(it.name, currentOffset)
            FileEPCombo(it, parsed?.first ?: "", parsed?.second)
        }.sortedBy { it.episode }
        converted = entries
        mainGrid.setItems(entries)
    }
}

data class FileEPCombo(val file: File, val episode: String, val version: Int?)

class FileBrowserDialog(private val allowMultiple: Boolean = true, val onClose: (Set<File>) -> Unit) : Dialog() {
    private var selected: Set<File> = emptySet()

    init {
        setSizeFull()
        maxWidth = "800px"
        maxHeight = "600px"
        val rootData = FilesystemData(File("/var/Anime"), true)
        val provider = FilesystemDataProvider(rootData)
        treeGrid(provider) {
            setSizeFull()
            setSelectionMode(if (allowMultiple) Grid.SelectionMode.MULTI else Grid.SelectionMode.SINGLE)
            addComponentHierarchyColumn {
                horizontalLayout {
                    defaultVerticalComponentAlignment = FlexComponent.Alignment.END
                    FileTypeResolver.getIcon(it)
                    nativeLabel(it.name) {
                        setWidthFull()
                    }
                    if (!it.isDirectory)
                        nativeLabel(it.length().readableSize())
                }
            }
            addSelectionListener {
                selected = it.allSelectedItems
            }
        }
    }

    override fun onDetach(detachEvent: DetachEvent?) = onClose(selected)
}