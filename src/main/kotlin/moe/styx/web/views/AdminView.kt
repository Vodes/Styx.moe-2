package moe.styx.web.views

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setClassNames2
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.web.checkAuth
import moe.styx.web.components.authProgress
import moe.styx.web.components.initMediaComponent
import moe.styx.web.components.noAccess
import moe.styx.web.components.user.userListing
import moe.styx.web.createComponent
import moe.styx.web.getDBClient
import moe.styx.web.layout.MainLayout

@Route("admin", layout = MainLayout::class)
@PageTitle("Styx - Admin")
class AdminView : KComposite() {

    val root = ui {
        verticalLayout(false) {
            setClassNames2(LumoUtility.Margin.NONE, LumoUtility.Padding.NONE)
            authProgress()
        }.also { layout ->
            checkAuth(UI.getCurrent(), VaadinRequest.getCurrent(), 99, layout, notLoggedIn = { noAccess() }) {
                h2("Welcome, ${it.name}!")
                init(initAdminView())
            }
        }
    }

    private fun initAdminView() = createComponent {
        val dbClient = getDBClient()
        accordion {
            setWidthFull()
            panel("Media Management") {
                init(initMediaComponent(dbClient))
            }
            panel("User Management") {
                init(userListing(dbClient))
            }
        }.also {
            dbClient.closeConnection()
        }
    }
}