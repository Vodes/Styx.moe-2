package moe.styx.web.components.misc

import com.github.mvysny.karibudsl.v10.h3
import com.github.mvysny.karibudsl.v10.nativeLabel
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.kaributools.tooltip
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.extension.eqI
import moe.styx.common.extension.toBoolean
import moe.styx.common.util.launchGlobal
import moe.styx.db.getEntries
import moe.styx.db.getMedia
import moe.styx.db.getMediaWatched
import moe.styx.web.getDBClient

fun generateUnwatched(ui: UI) {
    launchGlobal {
        getDBClient().executeAndClose {
            val watched = getMediaWatched()
            val medias = getMedia()
            val entries = getEntries().filter { it.timestamp > 1641038423L } // 2022-01-01
            val unwatched = mutableListOf<MediaEntry>()
            for (entry in entries) {
                val wat = watched.filter { it.entryID eqI entry.GUID && it.maxProgress > 35 }
                if (wat.isEmpty())
                    unwatched.add(entry)
            }
            val mapped = unwatched.groupBy { it.mediaID }.map { map -> medias.find { it.GUID eqI map.key } to map.value }
                .filter { it.first != null && it.first!!.isSeries.toBoolean() }.sortedByDescending { it.first!!.added }
            ui.access { UnwatchedDialog(mapped).open() }
        }
    }
}


private class UnwatchedDialog(entries: List<Pair<Media?, List<MediaEntry>>>) : Dialog() {
    init {
        verticalLayout {
            h3("Unwatched Shows/Episodes") {
                addClassNames(LumoUtility.Padding.MEDIUM)
            }

            entries.forEachIndexed { index, pair ->
                if (pair.first == null) {
                    return@forEachIndexed
                }
                verticalLayout(false) {
                    if (index != 0)
                        addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_30)
                    addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Padding.Top.LARGE)
                    nativeLabel("${pair.first!!.name} (${pair.second.size})") {
                        tooltip = pair.second.sortedBy { it.entryNumber.toDoubleOrNull() ?: 0.0 }.take(10)
                            .joinToString { it.entryNumber } + if (pair.second.size > 10) "..." else ""
                    }
                }
            }
        }
    }
}