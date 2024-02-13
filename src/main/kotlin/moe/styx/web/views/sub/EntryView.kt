package moe.styx.web.views.sub

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.h3
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.*
import com.vaadin.flow.server.VaadinRequest
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.db.getEntries
import moe.styx.db.getMedia
import moe.styx.web.checkAuth
import moe.styx.web.components.authProgress
import moe.styx.web.components.entry.entryOverview
import moe.styx.web.getDBClient

@Route("/entry")
class EntryView : KComposite(), HasDynamicTitle, HasUrlParameter<String> {
    private var media: Media? = null
    private var entry: MediaEntry? = null

    val root = ui {
        verticalLayout {
            isSpacing = false
            isPadding = false
            setSizeFull()
            authProgress()
        }.apply {
            checkAuth(UI.getCurrent(), VaadinRequest.getCurrent(), parent = this) {
                if (media == null) {
                    h3("No parent media found.")
                } else
                    entryOverview(entry, media!!)
            }
        }
    }

    override fun getPageTitle(): String {
        return if (entry == null || media == null)
            "Create new Entry - Styx"
        else
            "Editing ${media!!.name} - ${entry!!.entryNumber} - Styx"
    }

    override fun setParameter(event: BeforeEvent, @OptionalParameter entryID: String?) {
        getDBClient().executeAndClose {
            if (!entryID.isNullOrBlank()) {
                entry = getEntries(mapOf("GUID" to entryID)).firstOrNull()
                if (entry != null)
                    media = getMedia(mapOf("GUID" to entry!!.mediaID)).firstOrNull()
            }
            val parameters = event.location.queryParameters.parameters
            if (parameters.containsKey("media") && media == null) {
                media = getMedia(mapOf("GUID" to parameters["media"]!!.first())).firstOrNull()
            }
        }
    }
}