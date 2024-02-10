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
import kotlinx.datetime.Clock
import moe.styx.common.data.Media
import moe.styx.common.extension.toBoolean
import moe.styx.common.extension.toInt
import moe.styx.db.StyxDBClient
import moe.styx.db.getMedia
import moe.styx.db.save
import moe.styx.web.Main.updateChanges
import moe.styx.web.components.entry.entryListing
import moe.styx.web.data.getAniListDataForID
import moe.styx.web.getDBClient
import moe.styx.web.getFirstIDFromMap
import moe.styx.web.replaceAll
import java.util.*

class MediaOverview(media: Media?) : KComposite() {
    private var internalMedia = media ?: Media(UUID.randomUUID().toString().uppercase(), "", "", "", "", "", "")
    private lateinit var metadataLayout: VerticalLayout
    private lateinit var imagesLayout: VerticalLayout
    private lateinit var mappingLayout: VerticalLayout
    private var wasChanged = false

    val root = ui {
        verticalLayout {
            h2(if (media == null) "Creating new Media" else "Editing ${media.name}")
            horizontalLayout(false) {
                defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                button("Fill from AniList") {
                    onLeftClick {
                        val id = internalMedia.getFirstIDFromMap(StackType.ANILIST)
                        if (id == null)
                            Notification.show("No AniList ID was found in the mapping.").also { return@onLeftClick }

                        val result = getAniListDataForID(id!!)
                        if (result == null)
                            Notification.show("Could not get data from AniList API.").also { return@onLeftClick }

                        internalMedia = internalMedia.copy(
                            nameJP = result!!.title.romaji,
                            nameEN = result.title.english,
                            synopsisEN = result.description,
                            genres = result.genres.joinToString(", "),
                            tags = result.tags.filter { it.rank > 60 && !it.isMediaSpoiler }.take(10).joinToString(", ") { it.name }
                        )
                        updateTabs()
                    }
                }
                checkBox("Movie") {
                    value = !internalMedia.isSeries.toBoolean()
                    addValueChangeListener {
                        internalMedia = internalMedia.copy(isSeries = (!it.value).toInt())
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
                        add(MetadataView(internalMedia) { internalMedia = it; wasChanged = true; internalMedia })
                    }
                }
                tab("Images") {
                    imagesLayout = verticalLayout {
                        isPadding = false
                        isSpacing = false
                        add(ThumbnailComponent(internalMedia) { internalMedia = it; wasChanged = true; internalMedia })
                    }
                }
                tab("Mapping") {
                    mappingLayout = verticalLayout {
                        isPadding = false
                        isSpacing = false
                        add(MappingOverview(internalMedia) { internalMedia = it; wasChanged = true; internalMedia })
                    }
                }
                val entryCount = getDBClient().executeGet { genericCount("MediaEntry", mapOf("mediaID" to internalMedia.GUID)) }
                val entryTab = Tab(Span("Entries").apply { addClassNames(Padding.Right.SMALL) },
                    Badge("$entryCount").apply { addClassNames(Padding.Horizontal.SMALL) })
                if (entryCount < 1)
                    entryTab.isEnabled = false

                add(entryTab, entryListing(internalMedia))

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
                    onLeftClick {
                        getDBClient().executeAndClose {
                            save(internalMedia)
                            updatePrequelSequel(this, internalMedia)
                        }
                        updateChanges(media = Clock.System.now().epochSeconds)
                        UI.getCurrent().page.history.back()
                    }
                }
                button("Delete") {
                    addThemeVariants(ButtonVariant.LUMO_ERROR)
                    onLeftClick {
                        UI.getCurrent().page.history.back()
                    }
                }
            }
        }
    }

    private fun updateTabs() {
        metadataLayout.replaceAll {
            MetadataView(internalMedia) { internalMedia = it; wasChanged = true; internalMedia }
        }
        imagesLayout.replaceAll {
            ThumbnailComponent(internalMedia) { internalMedia = it; wasChanged = true; internalMedia }
        }
        mappingLayout.replaceAll {
            MappingOverview(internalMedia) { internalMedia = it; wasChanged = true; internalMedia }
        }
    }
}

private fun updatePrequelSequel(dbClient: StyxDBClient, media: Media) {
    if (!media.prequel.isNullOrBlank()) {
        val prequelMedia = dbClient.getMedia(mapOf("GUID" to media.prequel!!)).firstOrNull()
        if (prequelMedia != null) {
            dbClient.save(prequelMedia.copy(sequel = media.GUID))
        }
    } else {
        val prequelMedia = dbClient.getMedia(mapOf("sequel" to media.GUID)).firstOrNull()
        if (prequelMedia != null) {
            dbClient.save(prequelMedia.copy(sequel = ""))
        }
    }
    if (!media.sequel.isNullOrBlank()) {
        val sequelMedia = dbClient.getMedia(mapOf("GUID" to media.sequel!!)).firstOrNull()
        if (sequelMedia != null) {
            dbClient.save(sequelMedia.copy(prequel = media.GUID))
        }
    } else {
        val sequelMedia = dbClient.getMedia(mapOf("prequel" to media.GUID)).firstOrNull()
        if (sequelMedia != null) {
            dbClient.save(sequelMedia.copy(prequel = ""))
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