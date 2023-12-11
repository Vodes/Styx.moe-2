package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.ScaleMethod
import com.sksamuel.scrimage.webp.WebpWriter
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.FlexLayout
import com.vaadin.flow.component.upload.receivers.MemoryBuffer
import com.vaadin.flow.theme.lumo.LumoUtility.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.styx.types.Image
import moe.styx.types.Media
import moe.styx.types.json
import moe.styx.types.toBoolean
import moe.styx.web.Main
import moe.styx.web.createComponent
import moe.styx.web.data.TmdbImage
import moe.styx.web.data.tmdbImageQuery
import moe.styx.web.httpClient
import moe.styx.web.isWindows
import org.vaadin.lineawesome.LineAwesomeIcon
import java.io.File
import java.util.*

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

    override fun onDetach(detachEvent: DetachEvent?) = onClose(current)
}

class TMDBImageDialog(val id: Int, val tv: Boolean, val thumbnail: Boolean, val onClose: (Image?) -> Unit) : Dialog() {
    private var selected: Image? = null

    init {
        setWidthFull()
        maxWidth = "700px"
        verticalLayout {
            height = "800px"
            val result = tmdbImageQuery(this@TMDBImageDialog.id, tv)
            if (result != null) {
                (if (thumbnail) result.posters else result.backdrops).sortedByDescending { it.voteCount }.forEach {
                    add(imagePreview(it) {
                        runBlocking {
                            val response = httpClient.get(it.getURL())
                            if (response.status != HttpStatusCode.OK)
                                return@runBlocking

                            if (isWindows()) {
                                selected = Image(UUID.randomUUID().toString().uppercase(), externalURL = it.getURL(), type = if (thumbnail) 0 else 1)
                                close()
                                return@runBlocking
                            }

                            val stream = response.bodyAsChannel().toInputStream()
                            var image = ImmutableImage.loader().fromStream(stream).also {
                                runCatching {
                                    stream.close()
                                }
                            }

                            // Make thumbnails smaller
                            if (image.ratio() < 1 && image.height > 700)
                                image = image.scaleToHeight(700, ScaleMethod.Bicubic)
                            // Make banners smaller and since they're in landscape, go by width
                            else if (image.ratio() > 1 && image.width > 1600)
                                image = image.scaleToWidth(1600, ScaleMethod.Bicubic)

                            val guid = UUID.randomUUID().toString().uppercase()
                            val output = File(Main.config.imageDir, "$guid.webp")
                            image.output(WebpWriter.DEFAULT.withQ(100).withM(6), output)
                            selected = Image(guid, hasWEBP = 1, type = if (thumbnail) 0 else 1)
                            close()
                        }
                    })
                }
            } else {
                h3("Could not resolve tmdb images.")
            }
        }
    }

    override fun onDetach(detachEvent: DetachEvent?) = onClose(selected)
}

fun imagePreview(img: TmdbImage, onSelect: () -> Unit) = createComponent {
    val thumb = img.aspectRatio < 1
    flexLayout {
        addClassNames(Border.BOTTOM, BorderColor.CONTRAST_30, Padding.Bottom.MEDIUM, Gap.MEDIUM)
        setWidthFull()
        flexDirection = if (thumb) FlexLayout.FlexDirection.ROW else FlexLayout.FlexDirection.COLUMN
        maxHeight = if (thumb) "320px" else "365px"
        image(img.getURL()) {
            maxHeight = "280px"
            onLeftClick {
                UI.getCurrent().page.open(img.getURL())
            }
        }
        flexLayout {
            setHeightFull()
            flexDirection = FlexLayout.FlexDirection.COLUMN
            var innerHtml = "Language: ${img.languageCode?.uppercase() ?: "Unknown"}<br>Votes: ${img.voteCount}" +
                    "<br>Average Vote: ${img.voteAverage}<br>Resolution: ${img.width}x${img.height}"
            if (!thumb)
                innerHtml = innerHtml.replace("<br>", " — ")
            htmlSpan(innerHtml)
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