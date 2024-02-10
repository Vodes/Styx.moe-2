package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.AnchorTarget
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import moe.styx.common.data.BasicMapping
import moe.styx.common.data.Media
import moe.styx.common.data.TMDBMapping
import moe.styx.common.extension.toBoolean
import moe.styx.web.data.searchAniList
import moe.styx.web.data.tmdb.getTmdbMetadata
import moe.styx.web.data.tmdb.tmdbFindMedia
import moe.styx.web.replaceAll

class MappingSearchDialog(val parent: MappingStack, val media: Media) : Dialog() {
    private lateinit var resultLayout: VerticalLayout
    private lateinit var searchField: TextField

    init {
        verticalLayout {
            searchField = textField("Search") {
                value = media.nameEN ?: media.name
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener {
                    updateResults(it.value)
                }
            }
            resultLayout = verticalLayout { }
        }.also {
            updateResults(media.nameEN ?: media.name)
        }
    }

    private fun updateResults(search: String) {
        if (this@MappingSearchDialog.parent.type == StackType.TMDB) {
            val results = tmdbFindMedia(search, media.isSeries.toBoolean())
            if (results.isEmpty())
                resultLayout.replaceAll { h3("Could not find anything for this search.") }.also { return }
            resultLayout.removeAll()
            results.forEach { meta ->
                resultLayout.add(
                    verticalLayout(false) {
                        anchor(
                            "https://www.themoviedb.org/${if (media.isSeries.toBoolean()) "tv" else "movie"}/${meta.id}",
                            meta.name
                        ).apply { setTarget(AnchorTarget.BLANK) }
                        button("Add") {
                            onLeftClick {
                                val entryMeta = getTmdbMetadata(meta.id, media.isSeries.toBoolean())
                                if ((entryMeta?.numberSeasons ?: 1) > 1) {
                                    Notification.show(
                                        "This series has ${entryMeta?.numberSeasons ?: 2} seasons.",
                                        1200,
                                        Notification.Position.TOP_CENTER
                                    )
                                }
                                this@MappingSearchDialog.parent.addEntry(
                                    StackEntry(
                                        this@MappingSearchDialog.parent,
                                        TMDBMapping(remoteID = meta.id)
                                    )
                                )
                            }
                        }
                    }
                )
            }
        } else {
            val results = searchAniList(search)
            if (results.isEmpty())
                resultLayout.replaceAll { h3("Could not find anything for this search.") }.also { return }
            results.forEach { meta ->
                resultLayout.add(
                    verticalLayout(false) {
                        anchor(meta.listingURL(), meta.anyTitle()).apply { setTarget(AnchorTarget.BLANK) }
                        button("Add") {
                            onLeftClick {
                                this@MappingSearchDialog.parent.addEntry(
                                    StackEntry(this@MappingSearchDialog.parent, BasicMapping(remoteID = meta.id))
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

