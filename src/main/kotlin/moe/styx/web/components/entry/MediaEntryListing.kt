package moe.styx.web.components.entry

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.tooltip
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.confirmdialog.ConfirmDialog
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.theme.lumo.LumoUtility
import kotlinx.serialization.encodeToString
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.extension.equalsAny
import moe.styx.common.extension.readableSize
import moe.styx.common.extension.toBoolean
import moe.styx.common.prettyPrintJson
import moe.styx.db.tables.ImageTable
import moe.styx.db.tables.MediaEntryTable
import moe.styx.downloader.utils.getMediaInfo
import moe.styx.web.createComponent
import moe.styx.web.data.sendDiscordHookEmbed
import moe.styx.web.dbClient
import moe.styx.web.getURL
import moe.styx.web.topNotification
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.vaadin.lineawesome.LineAwesomeIcon
import java.io.File

fun entryListing(media: Media) = createComponent {
    verticalLayout {
        val episodes = dbClient.transaction { MediaEntryTable.query { selectAll().where { mediaID eq media.GUID }.toList() } }
            .sortedBy { it.entryNumber.toDoubleOrNull() ?: 0.0 }
        horizontalLayout(false) {
            button("Fetch TMDB Metadata") {
                onClick {
                    TMDBMetaDialog(media).open()
                }
            }
            button("Add new") {
                onClick {
                    UI.getCurrent().navigate("/entry?media=${media.GUID}")
                }
            }
            if(!media.isSeries.toBoolean() && episodes.isNotEmpty()) {
                button("Notify Discord") {
                    onClick {
                        val image = media.thumbID?.let {
                            dbClient.transaction {
                                ImageTable.query { selectAll().where { GUID eq it }.toList() }.firstOrNull()
                            }
                        }
                        if(image == null) {
                            topNotification("This media has no thumbnail!")
                            return@onClick
                        }
                        sendDiscordHookEmbed("Movie added", media.name, image.getURL())
                    }
                }
            }
        }
        episodes.forEachIndexed { index, entry ->
            verticalLayout(true) {
                if (index != 0)
                    addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_30, LumoUtility.Padding.Top.MEDIUM)
                h3(entry.entryNumber + (if (entry.nameEN.isNullOrBlank()) "" else " - ${entry.nameEN}"))
                if (!entry.nameDE.isNullOrBlank())
                    nativeLabel(entry.nameDE)
                if (!entry.synopsisEN.isNullOrBlank())
                    details("Synopsis") { nativeLabel(entry.synopsisEN) }
                if (!entry.synopsisDE.isNullOrBlank())
                    details("Synopsis DE") { nativeLabel(entry.synopsisDE) }
                val file = File(entry.filePath)
                htmlSpan("<b>File:</b> ${file.name}") {
                    tooltip = file.parentFile.absolutePath
                }
                if (!entry.originalName.isNullOrBlank() && !entry.originalName.equalsAny(file.name, file.nameWithoutExtension))
                    htmlSpan("<b>Original File:</b> ${entry.originalName}")
                horizontalLayout(false) {
                    defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                    iconButton(LineAwesomeIcon.PEN_SOLID.create()) {
                        onClick {
                            UI.getCurrent().navigate("/entry/${entry.GUID}?media=${media.GUID}")
                        }
                    }
                    iconButton(LineAwesomeIcon.TRASH_SOLID.create()) {
                        addThemeVariants(ButtonVariant.LUMO_ERROR)
                        onClick { entryDeleteDialog(entry) }
                    }
                    iconButton(LineAwesomeIcon.INFO_CIRCLE_SOLID.create()) {
                        onClick { entryMediaInfoDialog(entry) }
                    }
                    h5("Size: ${entry.fileSize.readableSize()}")
                }
            }
        }
    }
}

fun entryDeleteDialog(mediaEntry: MediaEntry) {
    ConfirmDialog().apply {
        setHeader("Do you really want to delete this?")
        setText(htmlSpan("\"onlyFiles\" only cause the downloader to re-download it.<br>If a profile is given of course."))
        setRejectText("only Files")
        setRejectable(true)
        setCancelable(true)
        isCloseOnEsc = false
        setConfirmText("Yes")
        setCancelText("No")

        addRejectListener {
            val file = File(mediaEntry.filePath)
            if (file.exists() && file.delete())
                dbClient.transaction {
                    MediaEntryTable.upsertItem(mediaEntry.copy(originalName = ""))
                }
        }
        addConfirmListener {
            val file = File(mediaEntry.filePath)
            if (file.exists())
                file.delete()
            dbClient.transaction { MediaEntryTable.deleteWhere { GUID eq mediaEntry.GUID } }
        }
    }.open()
}

fun entryMediaInfoDialog(mediaEntry: MediaEntry) {
    val entryFile = File(mediaEntry.filePath)
    val mediainfo = if (entryFile.exists()) entryFile.getMediaInfo() else null
    Dialog().apply {
        setSizeFull()
        maxWidth = "800px"
        maxHeight = "600px"
        verticalLayout {
            setSizeFull()
            if (!entryFile.exists() || mediainfo == null) {
                h3("Could not find file.")
                return@verticalLayout
            }
            textArea("MediaInfo") {
                setSizeFull()
                isReadOnly = true
                value = prettyPrintJson.encodeToString(mediainfo)
            }
        }
    }.open()
}