package moe.styx.web.components.entry

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.tooltip
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.confirmdialog.ConfirmDialog
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.extension.equalsAny
import moe.styx.db.delete
import moe.styx.db.getEntries
import moe.styx.db.save
import moe.styx.web.createComponent
import moe.styx.web.getDBClient
import moe.styx.web.readableSize
import org.vaadin.lineawesome.LineAwesomeIcon
import java.io.File

fun entryListing(media: Media) = createComponent {
    verticalLayout {
        horizontalLayout(false) {
            button("Fetch TMDB Metadata") {
                onLeftClick {
                    TMDBMetaDialog(media).open()
                }
            }
            button("Add new") {
                onLeftClick {
                    UI.getCurrent().navigate("/entry?media=${media.GUID}")
                }
            }
        }
        val episodes = getDBClient().executeGet { getEntries(mapOf("mediaID" to media.GUID)) }.sortedBy { it.entryNumber.toDoubleOrNull() ?: 0.0 }
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
                        onLeftClick {
                            UI.getCurrent().navigate("/entry/${entry.GUID}?media=${media.GUID}")
                        }
                    }
                    iconButton(LineAwesomeIcon.TRASH_SOLID.create()) {
                        addThemeVariants(ButtonVariant.LUMO_ERROR)
                        onLeftClick { entryDeleteDialog(entry) }
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
                getDBClient().executeAndClose {
                    save(mediaEntry.copy(originalName = ""))
                }
        }
        addConfirmListener {
            val file = File(mediaEntry.filePath)
            if (file.exists())
                file.delete()
            getDBClient().executeAndClose { delete(mediaEntry) }
        }
    }.open()
}