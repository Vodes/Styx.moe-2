package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.FlexLayout
import com.vaadin.flow.component.upload.receivers.MemoryBuffer
import com.vaadin.flow.theme.lumo.LumoUtility.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.styx.types.Image
import moe.styx.types.Media
import moe.styx.types.json
import moe.styx.types.toBoolean
import moe.styx.web.createComponent
import moe.styx.web.data.TmdbImage
import moe.styx.web.data.tmdbImageQuery
import org.vaadin.lineawesome.LineAwesomeIcon

class ImageDialog(val media: Media, val thumbnail: Boolean, val onClose: (Image?) -> Unit) : Dialog() {
    private var current: Image? = null

    init {
        setWidthFull()
        maxWidth = "700px"
        verticalLayout {
            isPadding = false
            isSpacing = false
            setWidthFull()
            h3("Download from URL") { addClassNames(Padding.Bottom.MEDIUM) }
            verticalLayout {
                addClassNames(Padding.Horizontal.SMALL)
                setWidthFull()
                horizontalLayout {
                    setWidthFull()
                    textField {
                        setWidthFull()
                    }
                    iconButton(LineAwesomeIcon.DOWNLOAD_SOLID.create())
                }
                horizontalLayout {
                    isPadding = false

                    button("From Anilist") {
                        addClassNames(Padding.Horizontal.SMALL)
                        onLeftClick {
                            println("meme")
                        }
                    }
                    button("Choose from TMDB") {
                        addClassNames(Padding.Horizontal.SMALL)
                        onLeftClick {
                            val id = getFirstIDFromMap(media, StackType.TMDB)
                            if (id == null) {
                                Notification.show("No TMDB ID found in mappings!")
                                return@onLeftClick
                            }
                            TMDBImageDialog(id, media.isSeries.toBoolean(), thumbnail) {
                                current = it
                                if (it != null)
                                    close()
                            }.open()
                        }
                    }
                }
            }
            h3("Upload File") { addClassNames(Padding.Vertical.MEDIUM) }
            upload(MemoryBuffer()) {
                addClassNames(Padding.Horizontal.SMALL)
                maxFiles = 1
                setAcceptedFileTypes("image/png", "image/webp", "image/jpeg")
                addFileRejectedListener {
                    Notification.show("Invalid file!", 1500, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR)
                }
            }
        }
    }

    override fun onDetach(detachEvent: DetachEvent?) {
        onClose(current)
    }
}

class TMDBImageDialog(val id: Int, val tv: Boolean, val thumbnail: Boolean, val onClose: (Image?) -> Unit) : Dialog() {
    init {
        setWidthFull()
        maxWidth = "700px"
        verticalLayout {
            height = "800px"
            val result = tmdbImageQuery(this@TMDBImageDialog.id, tv)
            if (result != null) {
                (if (thumbnail) result.posters else result.backdrops).sortedByDescending { it.voteCount }.forEach {
                    add(imagePreview(it) {
                        println("Selected: ${it.getURL()}")
                    })
                }
            } else {
                h3("Could not resolve tmdb images.")
            }
        }
    }
}

fun imagePreview(img: TmdbImage, onSelect: () -> Unit) = createComponent {
    horizontalLayout {
        addClassNames(Border.BOTTOM, BorderColor.CONTRAST_30, Padding.Bottom.MEDIUM)
        setWidthFull()
        defaultVerticalComponentAlignment = FlexComponent.Alignment.START
        maxHeight = "320px"
        image(img.getURL()) {
            maxHeight = "300px"
            onLeftClick {
                UI.getCurrent().page.open(img.getURL())
            }
        }
        flexLayout {
            setHeightFull()
            flexDirection = FlexLayout.FlexDirection.COLUMN
            htmlSpan(
                " Language: ${img.languageCode?.uppercase() ?: "Unknown"}<br>Votes: ${img.voteCount}" +
                        "<br>Average Vote: ${img.voteAverage}<br>Resolution: ${img.width} x ${img.height}"
            )
            button("Select") {
                maxWidth = "110px"
                addClassNames(Margin.Top.AUTO)
                onLeftClick {
                    onSelect()
                }
            }
        }
    }
}

private fun getFirstIDFromMap(media: Media, type: StackType): Int? {
    val mappingJson = media.metadataMap?.let {
        if (it.isBlank())
            return@let null
        json.decodeFromString<JsonObject>(it)
    } ?: return null
    val mapEntries = mappingJson[type.key]?.jsonObject?.entries ?: return null
    var value = mapEntries.firstOrNull()?.value?.jsonPrimitive?.content ?: return null
    if (value.contains("/"))
        value = value.split("/")[0]
    return (if (value.contains(",")) value.split(",")[0] else value).toIntOrNull()
}