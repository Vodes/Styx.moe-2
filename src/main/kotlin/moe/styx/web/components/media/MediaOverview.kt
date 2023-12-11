package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.karibudsl.v23.tab
import com.github.mvysny.karibudsl.v23.tabSheet
import com.github.mvysny.kaributools.Badge
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.theme.lumo.LumoUtility.Padding
import moe.styx.db.save
import moe.styx.types.Media
import moe.styx.web.getDBClient
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

                add(entryTab, VerticalLayout())

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
                        getDBClient().executeAndClose { save(internalMedia) }
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

@VaadinDsl
fun (@VaadinDsl HasComponents).mediaOverview(
    media: Media? = null,
    block: (@VaadinDsl MediaOverview).() -> Unit = {}
) = init(
    MediaOverview(media), block
)