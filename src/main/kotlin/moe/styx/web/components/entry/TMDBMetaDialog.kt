package moe.styx.web.components.entry

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.theme.lumo.LumoUtility
import com.vaadin.flow.theme.lumo.LumoUtility.Padding
import moe.styx.common.data.MappingCollection
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.data.TMDBMapping
import moe.styx.common.extension.padString
import moe.styx.common.json
import moe.styx.db.getEntries
import moe.styx.web.*
import moe.styx.web.data.tmdb.TmdbEpisode

class TMDBMetaDialog(val media: Media) : Dialog() {

    init {
        setSizeFull()
        maxWidth = "750px"
        maxHeight = "94%"
        val collection = runCatching { json.decodeFromString<MappingCollection>(media.metadataMap!!) }.getOrNull()

        verticalLayout main@{
            setSizeFull()
            if (collection == null) {
                h2("Could not find mapping for this media.")
                return@main
            }
            val episodes = getDBClient().executeGet { getEntries(mapOf("mediaID" to media.GUID)) }
            val test = episodes.associateWith { collection.getMappingForEpisode(it.entryNumber) as TMDBMapping? }
            val grouped = episodes.groupBy { collection.getMappingForEpisode(it.entryNumber) as TMDBMapping? }

            grouped.forEach { (mapping, entries) ->
                verticalLayout(false) {
                    addClassNames(LumoUtility.Border.TOP)
                    style.set("border-width", "thick")

                    if (mapping == null) {
                        htmlSpan("Could not find mapping for episodes:<br>${entries.map { it.entryNumber }}") {
                            addClassNames(Padding.Vertical.MEDIUM, LumoUtility.Border.ALL)
                        }
                        return@verticalLayout
                    }
                    val (metaEN, metaDE) = mapping.getRemoteEpisodes { topNotification(it) }
                    entries.sortedBy { it.entryNumber.toDoubleOrNull() ?: 0.0 }.forEachIndexed { index, entry ->
                        val number = entry.entryNumber.toDouble() + mapping.offset
                        val epMetaEN = metaEN.find { (if (it.order != null) it.order + 1 else it.episodeNumber) == number.toInt() }
                        val epMetaDE = metaDE.find { (if (it.order != null) it.order + 1 else it.episodeNumber) == number.toInt() }
                        init(episodeMetaview(entry, index, epMetaEN, epMetaDE))
                    }
                }
            }
        }
    }
}

fun episodeMetaview(entry: MediaEntry, index: Int, epMetaEN: TmdbEpisode?, epMetaDE: TmdbEpisode?) = createComponent {
    verticalLayout(false) {
        if (index != 0)
            addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_30)
        if (epMetaEN == null) {
            h4("No remote episode for ${entry.entryNumber} found.") { addClassNames(Padding.Vertical.MEDIUM) }
        } else {
            val remote = if (epMetaEN.order != null) epMetaEN.order + 1 else epMetaEN.episodeNumber
            h4("EP${entry.entryNumber} | TMDB: ${remote.padString(2)}") {
                addClassNames(Padding.Top.MEDIUM)
            }
            verticalLayout(false) {
                addClassNames(Padding.Bottom.MEDIUM)
                nativeLabel("Title EN: ${epMetaEN.name}")
                nativeLabel("Title DE: ${epMetaDE?.name ?: "/"}")
                if (epMetaEN.overview.isNotBlank())
                    details("Summary EN") { nativeLabel(epMetaEN.overview) }
                if (!epMetaDE?.overview.isNullOrBlank())
                    details("Summary DE") { nativeLabel(epMetaDE!!.overview) }

            }
        }
    }
}