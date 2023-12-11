package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.theme.lumo.LumoUtility.Gap
import com.vaadin.flow.theme.lumo.LumoUtility.Padding
import moe.styx.db.delete
import moe.styx.db.getImages
import moe.styx.db.save
import moe.styx.types.Media
import moe.styx.web.deleteIfExists
import moe.styx.web.getDBClient
import moe.styx.web.getURL

class ThumbnailComponent(var media: Media, val mediaProvider: (Media) -> Media) : KComposite() {
    private val thumbFallback = "https://vodes.pw/i/Alex/ZTKosJEnJMuRpnr.png"
    private val bannerFallback = "https://archive.is/vdFTk/103fb85fb390e1401bf27bee19b656fe4d8355c5.jpg"
    var currentThumbnail: moe.styx.types.Image? = null
    var currentBanner: moe.styx.types.Image? = null

    lateinit var bannerImg: Image
    lateinit var thumbImg: Image

    val root = ui {
        val dbClient = getDBClient()
        currentThumbnail = dbClient.getImages(mapOf("GUID" to (media.thumbID ?: ""))).firstOrNull()
        currentBanner = dbClient.getImages(mapOf("GUID" to (media.bannerID ?: ""))).firstOrNull()
        verticalLayout {
            setWidthFull()
            h3("Thumbnail") { addClassNames(Padding.Horizontal.NONE, Padding.Bottom.SMALL, Padding.Top.MEDIUM) }
            verticalLayout {
                setWidthFull()
                addClassNames(Gap.SMALL)
                var url = thumbFallback
                if (currentThumbnail != null)
                    url = currentThumbnail!!.getURL()
                thumbImg = image(url, "Image failed to load!") {
                    maxHeight = "300px"
                }
                button(if (currentThumbnail != null) "Replace" else "Add") {
                    onLeftClick {
                        ImageDialog(media, true) { handleResult(it, true) }.open()
                    }
                }
            }
            h3("Banner") { addClassNames(Padding.Horizontal.NONE, Padding.Vertical.SMALL) }
            verticalLayout {
                setWidthFull()
                addClassNames(Gap.SMALL)
                var url = bannerFallback
                if (currentBanner != null)
                    url = currentBanner!!.getURL()
                bannerImg = image(url, "Image failed to load!") {
                    maxHeight = "300px"
                }
                button(if (currentBanner != null) "Replace" else "Add") {
                    onLeftClick {
                        ImageDialog(media, false) { handleResult(it, false) }.open()
                    }
                }
            }
        }.also { dbClient.closeConnection() }
    }

    private fun handleResult(img: moe.styx.types.Image?, thumb: Boolean) {
        if (img == null)
            return

        val dbClient = getDBClient()
        val saved = dbClient.save(img)
        if (thumb) {
            val deleted = currentThumbnail?.let { dbClient.delete(currentThumbnail!!) } ?: true
            if (saved && deleted) {
                currentThumbnail?.deleteIfExists()
                media = mediaProvider(media.copy(thumbID = img.GUID))
                currentThumbnail = img
            }
        } else {
            val deleted = currentBanner?.let { dbClient.delete(currentBanner!!) } ?: true
            if (saved && deleted) {
                currentBanner?.deleteIfExists()
                media = mediaProvider(media.copy(bannerID = img.GUID))
                currentBanner = img
            }
        }
        dbClient.closeConnection()
        thumbImg.src = currentThumbnail?.getURL() ?: thumbFallback
        bannerImg.src = currentBanner?.getURL() ?: bannerFallback
    }

}