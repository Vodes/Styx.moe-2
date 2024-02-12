package moe.styx.web.components.entry

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.common.data.Media
import moe.styx.db.getEntries
import moe.styx.web.createComponent
import moe.styx.web.getDBClient
import moe.styx.web.readableSize
import moe.styx.web.topNotification
import org.vaadin.lineawesome.LineAwesomeIcon

fun entryListing(media: Media) = createComponent {
    verticalLayout {
        button("Fetch TMDB Metadata") {
            onLeftClick {
                TMDBMetaDialog(media).open()
            }
        }
        val episodes = getDBClient().executeGet { getEntries(mapOf("mediaID" to media.GUID)) }.sortedBy { it.entryNumber.toDoubleOrNull() ?: 0.0 }
        episodes.forEachIndexed { index, entry ->
            verticalLayout(false) {
                if (index != 0)
                    addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_30, LumoUtility.Padding.Top.MEDIUM)
                h3(entry.entryNumber + (if (entry.nameEN.isNullOrBlank()) "" else " - ${entry.nameEN}"))
                if (!entry.nameDE.isNullOrBlank())
                    nativeLabel(entry.nameDE)
                if (!entry.synopsisEN.isNullOrBlank())
                    details("Synopsis") { nativeLabel(entry.synopsisEN) }
                if (!entry.synopsisDE.isNullOrBlank())
                    details("Synopsis DE") { nativeLabel(entry.synopsisDE) }
                htmlSpan("<b>File:</b> ${entry.filePath}")
                htmlSpan("<b>Original File:</b> ${entry.originalName}")
                horizontalLayout(false) {
                    defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                    iconButton(LineAwesomeIcon.PEN_SOLID.create()) {
                        onLeftClick { topNotification("Not implemented yet.") }
                    }
                    iconButton(LineAwesomeIcon.TRASH_SOLID.create()) {
                        addThemeVariants(ButtonVariant.LUMO_ERROR)
                        onLeftClick { topNotification("Not implemented yet.") }
                    }
                    h5("Size: ${entry.fileSize.readableSize()}")
                }
            }
        }
    }
}