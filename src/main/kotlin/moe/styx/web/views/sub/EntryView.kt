package moe.styx.web.views.sub

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.h3
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.*
import com.vaadin.flow.server.VaadinRequest
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.db.tables.MediaEntryTable
import moe.styx.db.tables.MediaTable
import moe.styx.web.checkAuth
import moe.styx.web.components.authProgress
import moe.styx.web.components.entry.entryOverview
import moe.styx.web.dbClient
import org.jetbrains.exposed.sql.selectAll

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
        if (!entryID.isNullOrEmpty()) {
            dbClient.transaction {
                val entryLocal = MediaEntryTable.query { selectAll().where { GUID eq entryID.split("?")[0] }.toList() }.firstOrNull()
                if (entryLocal != null) {
                    media = MediaTable.query { selectAll().where { GUID eq entryLocal.mediaID }.toList() }.firstOrNull()
                }
                entry = entryLocal
            }
        }
        val parameters = event.location.queryParameters.parameters
        if (parameters.containsKey("media") && media == null) {
            media = dbClient.transaction { MediaTable.query { selectAll().where { GUID eq parameters["media"]!!.first() }.toList() }.firstOrNull() }
        }
    }
}