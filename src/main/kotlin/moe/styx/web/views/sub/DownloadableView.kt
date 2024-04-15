package moe.styx.web.views.sub

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.h2
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.kaributools.navigateTo
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinRequest
import moe.styx.common.data.DownloaderTarget
import moe.styx.common.data.Media
import moe.styx.db.tables.DownloaderTargetsTable
import moe.styx.db.tables.MediaTable
import moe.styx.web.checkAuth
import moe.styx.web.components.authProgress
import moe.styx.web.components.downloadable.DownloadableOverview
import moe.styx.web.dbClient
import moe.styx.web.views.AdminView
import org.jetbrains.exposed.sql.selectAll

@Route("download")
@PageTitle("Styx - Downloader")
class DownloadableView : KComposite(), HasUrlParameter<String> {
    private var media: Media? = null
    private var target: DownloaderTarget? = null

    val root = ui {
        verticalLayout {
            isPadding = false
            isSpacing = false
            authProgress()
        }.also { vert ->
            checkAuth(UI.getCurrent(), VaadinRequest.getCurrent(), minPerms = 99, parent = vert) {
                target?.let { DownloadableOverview(it, media!!) } ?: h2("Invalid media association!")
            }
        }
    }

    override fun setParameter(event: BeforeEvent, id: String) {
        media = dbClient.transaction { MediaTable.query { selectAll().where { GUID eq id }.toList() }.firstOrNull() }
        if (media == null) {
            navigateTo<AdminView>()
            return
        }
        target = dbClient.transaction { DownloaderTargetsTable.query { selectAll().where { mediaID eq id }.toList() }.firstOrNull() }
            ?: DownloaderTarget(media!!.GUID)
    }
}