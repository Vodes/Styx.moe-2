package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.karibudsl.v23.tab
import com.github.mvysny.karibudsl.v23.tabSheet
import com.github.mvysny.kaributools.Badge
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.theme.lumo.LumoUtility.Padding
import moe.styx.common.data.Media
import moe.styx.common.data.tmdb.StackType
import moe.styx.common.data.tmdb.getFirstIDFromMap
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.common.extension.toBoolean
import moe.styx.common.extension.toInt
import moe.styx.db.tables.ChangesTable
import moe.styx.db.tables.MediaEntryTable
import moe.styx.db.tables.MediaTable
import moe.styx.web.components.LocalEditorState
import moe.styx.web.components.entry.entryListing
import moe.styx.web.data.getAniListDataForID
import moe.styx.web.dbClient
import moe.styx.web.newGUID
import moe.styx.web.replaceAll
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.vaadin.lineawesome.LineAwesomeIcon

class MediaOverview(media: Media?) : KComposite() {
    private val mediaState = LocalEditorState(
        media ?: Media(newGUID(), "", "", "", "", "", "", added = currentUnixSeconds())
    ) {
        wasChanged = true
    }
    private lateinit var metadataLayout: VerticalLayout
    private lateinit var imagesLayout: VerticalLayout
    private lateinit var mappingLayout: VerticalLayout
    private lateinit var entryLayout: VerticalLayout
    private var wasChanged = false

    val root = ui {
        verticalLayout {
            h2(if (media == null) "Creating new Media" else "Editing ${media.name}")
            horizontalLayout(false) {
                defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                button("Fill from AniList") {
                    onClick {
                        val id = mediaState.current().getFirstIDFromMap(StackType.ANILIST)
                        if (id == null)
                            Notification.show("No AniList ID was found in the mapping.").also { return@onClick }

                        val result = getAniListDataForID(id!!)
                        if (result == null)
                            Notification.show("Could not get data from AniList API.").also { return@onClick }

                        mediaState.update {
                            it.copy(
                            nameJP = result!!.title.romaji,
                            nameEN = result.title.english,
                            synopsisEN = result.description,
                            genres = result.genres.joinToString(", "),
                            tags = result.tags.filter { it.rank > 60 && !it.isMediaSpoiler }.take(10).joinToString(", ") { it.name }
                        )
                        }
                        updateTabs()
                    }
                }
                iconButton(LineAwesomeIcon.FAST_FORWARD_SOLID.create()) {
                    setTooltipText("Quick Add (Anilist)")
                    onClick {
                        QuickAddDialog(mediaState.current()) {
                            mediaState.replace(it)
                            updateTabs()
                        }.open()
                    }
                }
                checkBox("Movie") {
                    value = !mediaState.current().isSeries.toBoolean()
                    addValueChangeListener {
                        mediaState.update { media -> media.copy(isSeries = (!it.value).toInt()) }
                        updateTabs()
                    }
                }
            }
            tabSheet {
                setSizeFull()
                tab("Metadata") {
                    metadataLayout = verticalLayout {
                        isPadding = false
                        isSpacing = false
                        setWidthFull()
                        add(MetadataView(mediaState))
                    }
                }
                tab("Images") {
                    imagesLayout = verticalLayout {
                        isPadding = false
                        isSpacing = false
                        add(ThumbnailComponent(mediaState))
                    }
                }
                tab("Mapping") {
                    mappingLayout = verticalLayout {
                        isPadding = false
                        isSpacing = false
                        add(MappingOverview(mediaState))
                    }
                }
                val entryCount = dbClient.transaction { MediaEntryTable.selectAll().where { MediaEntryTable.mediaID eq mediaState.current().GUID }.count() }
                val entryTab = Tab(
                    Span("Entries").apply { addClassNames(Padding.Right.SMALL) },
                    Badge("$entryCount").apply { addClassNames(Padding.Horizontal.SMALL) })
                entryLayout = VerticalLayout().apply {
                    setSizeFull()
                    init(entryListing(mediaState.current()))
                }
                add(entryTab, entryLayout)

                // Here I'm just making sure every layout has a reference to the latest media instance(?)
                addSelectedChangeListener {
                    if (wasChanged) {
                        updateTabs()
                        wasChanged = false
                    }
                }
            }
            horizontalLayout {
                button("Save") {
                    addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SUCCESS)
                    onClick {
                        dbClient.transaction {
                            MediaTable.upsertItem(mediaState.current())
                            updatePrequelSequel(mediaState.current())
                        }
                        dbClient.transaction { ChangesTable.setToNow(true, false) }
                        UI.getCurrent().page.history.back()
                    }
                }
                button("Delete") {
                    addThemeVariants(ButtonVariant.LUMO_ERROR)
                    onClick {
                        UI.getCurrent().page.history.back()
                    }
                }
            }
        }
    }

    private fun updateTabs() {
        metadataLayout.replaceAll {
            MetadataView(mediaState)
        }
        imagesLayout.replaceAll {
            ThumbnailComponent(mediaState)
        }
        mappingLayout.replaceAll {
            MappingOverview(mediaState)
        }
        entryLayout.replaceAll {
            entryListing(mediaState.current())
        }
    }
}

private fun updatePrequelSequel(media: Media) {
    if (!media.prequel.isNullOrBlank()) {
        val prequelMedia = dbClient.transaction { MediaTable.query { selectAll().where { GUID eq media.prequel!! }.toList() } }.firstOrNull()
        if (prequelMedia != null) {
            dbClient.transaction { MediaTable.upsertItem(prequelMedia.copy(sequel = media.GUID)) }
        }
    } else {
        val prequelMedia = dbClient.transaction { MediaTable.query { selectAll().where { sequel eq media.GUID }.toList() } }.firstOrNull()
        if (prequelMedia != null) {
            dbClient.transaction { MediaTable.upsertItem(prequelMedia.copy(sequel = "")) }
        }
    }
    if (!media.sequel.isNullOrBlank()) {
        val sequelMedia = dbClient.transaction { MediaTable.query { selectAll().where { GUID eq media.sequel!! }.toList() } }.firstOrNull()
        if (sequelMedia != null) {
            dbClient.transaction { MediaTable.upsertItem(sequelMedia.copy(prequel = media.GUID)) }
        }
    } else {
        val sequelMedia = dbClient.transaction { MediaTable.query { selectAll().where { prequel eq media.GUID }.toList() } }.firstOrNull()
        if (sequelMedia != null) {
            dbClient.transaction { MediaTable.upsertItem(sequelMedia.copy(prequel = "")) }
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).mediaOverview(
    media: Media? = null,
    block: (@VaadinDsl MediaOverview).() -> Unit = {}
) = init(
    MediaOverview(media), block
)
