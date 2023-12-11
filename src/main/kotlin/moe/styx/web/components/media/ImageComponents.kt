package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.db.getImages
import moe.styx.types.Media
import moe.styx.web.getDBClient
import moe.styx.web.getURL

class ThumbnailComponent(var media: Media, mediaProvider: (Media) -> Media) : KComposite() {
    val root = ui {
        val dbClient = getDBClient()
        val currentThumbnail = dbClient.getImages(mapOf("GUID" to (media.thumbID ?: ""))).firstOrNull()
        val currentBanner = dbClient.getImages(mapOf("GUID" to (media.bannerID ?: ""))).firstOrNull()
        verticalLayout {
            setWidthFull()
            h3("Thumbnail") { addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Bottom.SMALL, LumoUtility.Padding.Top.MEDIUM) }
            verticalLayout {
                setWidthFull()
                addClassNames(LumoUtility.Gap.SMALL)
                var url = "https://vodes.pw/i/Alex/ZTKosJEnJMuRpnr.png"
                if (currentThumbnail != null)
                    url = currentThumbnail.getURL()
                val thumbnail = image(url, "https://vodes.pw/i/Alex/ZTKosJEnJMuRpnr.png") {
                    maxHeight = "300px"
                }
                button(if (currentThumbnail != null) "Replace" else "Add") {
                    onLeftClick {
                        ImageDialog(media, true) {
                            if (it == null)
                                return@ImageDialog

                        }.open()
                    }
                }
            }
        }.also { dbClient.closeConnection() }
    }
}