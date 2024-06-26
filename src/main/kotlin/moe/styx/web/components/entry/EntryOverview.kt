package moe.styx.web.components.entry

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.data.MediaInfo
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.common.extension.readableSize
import moe.styx.common.extension.toBoolean
import moe.styx.common.extension.toInt
import moe.styx.db.tables.ChangesTable
import moe.styx.db.tables.MediaEntryTable
import moe.styx.db.tables.MediaInfoTable
import moe.styx.downloader.utils.getMediaInfo
import moe.styx.web.components.media.FileBrowserDialog
import moe.styx.web.dbClient
import moe.styx.web.newGUID
import moe.styx.web.topNotification
import org.vaadin.lineawesome.LineAwesomeIcon
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.floor

class EntryOverview(mediaEntry: MediaEntry?, media: Media) : KComposite() {
    private var entry = mediaEntry ?: MediaEntry(newGUID(), media.GUID, currentUnixSeconds(), "01", "", "", "", "", "", "", 0L, "")
    private lateinit var sizeField: TextField
    private lateinit var fileField: TextField

    val root = ui {
        verticalLayout {
            h2(if (mediaEntry == null) "Creating new Entry for ${media.name}" else "Editing ${media.name} - ${entry.entryNumber}")
            numberField("Episode") {
                min = 0.0
                value = entry.entryNumber.toDouble()
                step = 1.0
                isStepButtonsVisible = true
                valueChangeMode = ValueChangeMode.LAZY

                addValueChangeListener {
                    val ep = if (floor(it.value) == it.value) {
                        it.value.toInt().toString()
                    } else
                        String.format("%.1f", it.value)
                    entry = entry.copy(entryNumber = ep.padStart(2, '0'))
                }
            }
            dateTimePicker("Release Date & Time") {
                value = LocalDateTime.ofInstant(Instant.ofEpochSecond(entry.timestamp), ZoneId.systemDefault())
                step = Duration.of(30, ChronoUnit.MINUTES)
                addValueChangeListener {
                    entry = entry.copy(timestamp = it.value.atZone(ZoneId.systemDefault()).toInstant().epochSecond)
                }
            }
            flexLayout {
                setWidthFull()
                maxWidth = "1550px"
                addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL, "flex-container")
                justifyContentMode = FlexComponent.JustifyContentMode.EVENLY

                textField("Name EN") {
                    value = entry.nameEN ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { entry = entry.copy(nameEN = it.value) }
                    setWidthFull()
                }
                textField("Name DE") {
                    value = entry.nameDE ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { entry = entry.copy(nameDE = it.value) }
                    setWidthFull()
                }
            }
            flexLayout {
                setWidthFull()
                maxWidth = "1550px"
                addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL, "flex-container")
                justifyContentMode = FlexComponent.JustifyContentMode.EVENLY

                textArea("Synopsis EN") {
                    value = entry.synopsisEN ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { entry = entry.copy(synopsisEN = it.value) }
                    setWidthFull()
                    height = "230px"
                }

                textArea("Synopsis DE") {
                    value = entry.synopsisDE ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { entry = entry.copy(synopsisDE = it.value) }
                    setWidthFull()
                    height = "230px"
                }
            }
            verticalLayout {
                setWidthFull()
                maxWidth = "1550px"
                horizontalLayout(false) {
                    setWidthFull()
                    fileField = textField("File Path") {
                        setWidthFull()
                        value = entry.filePath
                        valueChangeMode = ValueChangeMode.LAZY
                        addValueChangeListener { entry = entry.copy(filePath = it.value) }
                    }
                    iconButton(LineAwesomeIcon.SYNC_SOLID.create()) {
                        setTooltipText("Update file size & mediainfo")
                        onClick {
                            val file = File(fileField.value)
                            if (!file.exists()) {
                                topNotification("The file couldn't be found.")
                                return@onClick
                            }
                            entry = entry.copy(fileSize = file.length())
                            sizeField.value = file.length().readableSize()
                            if (mediaEntry != null) {
                                dbClient.transaction {
                                    val mediainfo = file.getMediaInfo()
                                    if (mediainfo != null) {
                                        MediaInfoTable.upsertItem(
                                            MediaInfo(
                                                entry.GUID,
                                                mediainfo.videoCodec(),
                                                mediainfo.videoBitDepth(),
                                                mediainfo.videoResolution(),
                                                mediainfo.hasEnglishDub().toInt(),
                                                mediainfo.hasGermanDub().toInt(),
                                                mediainfo.hasGermanSub().toInt()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    iconButton(LineAwesomeIcon.FOLDER_OPEN_SOLID.create()) {
                        setTooltipText("Open file chooser")
                        onClick {
                            FileBrowserDialog(false) {
                                val file = it.firstOrNull() ?: return@FileBrowserDialog
                                fileField.value = file.absolutePath
                                sizeField.value = file.length().readableSize()
                                entry = entry.copy(filePath = file.absolutePath, fileSize = file.length())
                            }.open()
                        }
                    }
                }
                sizeField = textField("File Size") {
                    value = entry.fileSize.readableSize()
                    isEnabled = false
                }
                textField("Original File") {
                    setWidthFull()
                    value = entry.originalName ?: ""
                    isEnabled = false
                }
            }

            button("Save") {
                onClick {
                    if (dbClient.transaction { MediaEntryTable.upsertItem(entry) }.insertedCount.toBoolean()) {
                        val file = File(fileField.value)
                        if (file.exists() && mediaEntry != null) {
                            dbClient.transaction {
                                val mediainfo = file.getMediaInfo()
                                if (mediainfo != null) {
                                    MediaInfoTable.upsertItem(
                                        MediaInfo(
                                            entry.GUID,
                                            mediainfo.videoCodec(),
                                            mediainfo.videoBitDepth(),
                                            mediainfo.videoResolution(),
                                            mediainfo.hasEnglishDub().toInt(),
                                            mediainfo.hasGermanDub().toInt(),
                                            mediainfo.hasGermanSub().toInt()
                                        )
                                    )
                                }
                            }
                        }
                        dbClient.transaction { ChangesTable.setToNow(false, true) }
                        UI.getCurrent().page.history.back()
                    } else
                        topNotification("Failed to save entry!")
                }
            }
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).entryOverview(
    mediaEntry: MediaEntry? = null,
    media: Media,
    block: (@VaadinDsl EntryOverview).() -> Unit = {}
) = init(
    EntryOverview(mediaEntry, media), block
)