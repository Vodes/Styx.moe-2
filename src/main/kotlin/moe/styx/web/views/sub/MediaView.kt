package moe.styx.web.views.sub

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.*
import com.vaadin.flow.server.VaadinRequest
import moe.styx.db.getMedia
import moe.styx.types.Media
import moe.styx.web.checkAuth
import moe.styx.web.components.authProgress
import moe.styx.web.components.media.mediaOverview
import moe.styx.web.getDBClient

@Route("/media")
class MediaView : KComposite(), HasDynamicTitle, HasUrlParameter<String> {
    private var media: Media? = null

    val root = ui {
        verticalLayout {
            isSpacing = false
            isPadding = false
            setSizeFull()
            authProgress()
        }.apply {
            checkAuth(UI.getCurrent(), VaadinRequest.getCurrent(), parent = this) {
                mediaOverview(media)
            }
        }
    }

    override fun getPageTitle(): String {
        return if (media == null)
            "Create new Media - Styx"
        else
            "Editing ${media!!.name} - Styx"
    }

    override fun setParameter(event: BeforeEvent?, @OptionalParameter mediaID: String?) {
        if (mediaID != null)
            media = getDBClient().executeGet { getMedia(mapOf("GUID" to mediaID)) }.firstOrNull()
    }
}