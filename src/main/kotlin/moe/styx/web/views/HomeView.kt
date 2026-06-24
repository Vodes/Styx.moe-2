package moe.styx.web.views

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.h2
import com.github.mvysny.karibudsl.v10.nativeLabel
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.*
import kotlinx.coroutines.delay
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.common.util.launchThreaded
import moe.styx.db.tables.WebTempLinkTable
import moe.styx.web.components.user.userAndroidDownloadButtons
import moe.styx.web.components.user.userDesktopDownloadButtons
import moe.styx.web.components.user.userIOSDownloadButtons
import moe.styx.web.dbClient
import moe.styx.web.layout.MainLayout
import moe.styx.web.replaceAll
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@PageTitle("Styx - Home")
@Route("", layout = MainLayout::class)
class HomeView : KComposite(), HasUrlParameter<String> {
    private var showDownloads = false
    private lateinit var mainLayout: VerticalLayout

    val root = ui {
        verticalLayout {
            mainLayout = this
            isPadding = false
            h2("No clue what to put here")
        }
    }

    override fun setParameter(event: BeforeEvent, @OptionalParameter parameter: String?) {
        if (parameter.isNullOrBlank()) return
        val now = currentUnixSeconds()
        showDownloads = dbClient.transaction {
            WebTempLinkTable.query { selectAll().where { urlSegment eq parameter }.andWhere { expiresAt greater now }.toList() }.firstOrNull() != null
        }
        if (showDownloads) {
            val ui = UI.getCurrent()
            launchThreaded {
                delay(250.toDuration(DurationUnit.MILLISECONDS))
                ui.access {
                    mainLayout.replaceAll {
                        nativeLabel("This is a temporary download link. It will disable itself after this attempt.")
                        userDesktopDownloadButtons()
                        userAndroidDownloadButtons()
                        userIOSDownloadButtons()
                    }
                }
            }
            dbClient.transaction {
                WebTempLinkTable.deleteWhere { urlSegment eq parameter }
            }
        }
    }
}