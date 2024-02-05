package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.data.value.ValueChangeMode
import moe.styx.downloader.parsing.parseEpisodeAndVersion
import moe.styx.types.Media
import moe.styx.web.readableSize
import moe.styx.web.topNotification
import org.vaadin.filesystemdataprovider.FileTypeResolver
import org.vaadin.filesystemdataprovider.FilesystemData
import org.vaadin.filesystemdataprovider.FilesystemDataProvider
import java.io.File
import java.time.LocalDate

class ImportDialog(val media: Media) : Dialog() {
    private var selected: Set<File> = emptySet()
    private lateinit var layout: VerticalLayout
    private lateinit var firstEpDatePicker: DatePicker
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
                onLeftClick {
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
                // TODO: Discord Notifications... maybe
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
                onLeftClick {
                    // TODO: this
                    topNotification("Not implemented just yet.")
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

class FileBrowserDialog(val onClose: (Set<File>) -> Unit) : Dialog() {
    private var selected: Set<File> = emptySet()

    init {
        setSizeFull()
        maxWidth = "800px"
        maxHeight = "600px"
        val rootData = FilesystemData(File("/var/Anime"), true)
        val provider = FilesystemDataProvider(rootData)
        treeGrid(provider) {
            setSizeFull()
            setSelectionMode(Grid.SelectionMode.MULTI)
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