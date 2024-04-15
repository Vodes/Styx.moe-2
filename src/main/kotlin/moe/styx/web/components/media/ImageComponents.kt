package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.theme.lumo.LumoUtility.Gap
import com.vaadin.flow.theme.lumo.LumoUtility.Padding
import moe.styx.common.data.Media
import moe.styx.common.extension.toBoolean
import moe.styx.db.tables.ImageTable
import moe.styx.web.dbClient
import moe.styx.web.deleteIfExists
import moe.styx.web.getURL
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll

class ThumbnailComponent(var media: Media, val mediaProvider: (Media) -> Media) : KComposite() {
    private val thumbFallback = "https://vodes.pw/i/Alex/ZTKosJEnJMuRpnr.png"
    private val bannerFallback = "https://archive.is/vdFTk/103fb85fb390e1401bf27bee19b656fe4d8355c5.jpg"
    var currentThumbnail: moe.styx.common.data.Image? = null
    var currentBanner: moe.styx.common.data.Image? = null

    lateinit var bannerImg: Image
    lateinit var thumbImg: Image

    val root = ui {
        currentThumbnail = dbClient.transaction { ImageTable.query { selectAll().where { GUID eq (media.thumbID ?: "") }.toList() }.firstOrNull() }
        currentBanner = dbClient.transaction { ImageTable.query { selectAll().where { GUID eq (media.bannerID ?: "") }.toList() }.firstOrNull() }
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
        }
    }

    private fun handleResult(img: moe.styx.common.data.Image?, thumb: Boolean) {
        if (img == null)
            return

        val saved = dbClient.transaction { ImageTable.upsertItem(img) }.insertedCount.toBoolean()
        if (thumb) {
            val deleted =
                currentThumbnail?.let { dbClient.transaction { ImageTable.deleteWhere { GUID eq currentThumbnail!!.GUID }.toBoolean() } } ?: true
            if (saved && deleted) {
                currentThumbnail?.deleteIfExists()
                media = mediaProvider(media.copy(thumbID = img.GUID))
                currentThumbnail = img
            }
        } else {
            val deleted = currentBanner?.let { dbClient.transaction { ImageTable.deleteWhere { GUID eq currentBanner!!.GUID }.toBoolean() } } ?: true
            if (saved && deleted) {
                currentBanner?.deleteIfExists()
                media = mediaProvider(media.copy(bannerID = img.GUID))
                currentBanner = img
            }
        }
        thumbImg.src = currentThumbnail?.getURL() ?: thumbFallback
        bannerImg.src = currentBanner?.getURL() ?: bannerFallback
    }

}