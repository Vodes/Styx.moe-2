package moe.styx.web.views

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setClassNames2
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.*
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.web.checkAuth
import moe.styx.web.components.MediaGrid
import moe.styx.web.components.authProgress
import moe.styx.web.components.misc.generateUnwatched
import moe.styx.web.components.noAccess
import moe.styx.web.components.user.userListing
import moe.styx.web.createComponent
import moe.styx.web.layout.MainLayout
import moe.styx.web.topNotification

@Route("admin", layout = MainLayout::class)
@PageTitle("Styx - Admin")
class AdminView : KComposite(), HasUrlParameter<String> {
    private var query: String? = null

    val root = ui {
        verticalLayout(false) {
            setClassNames2(LumoUtility.Margin.NONE, LumoUtility.Padding.NONE)
            authProgress()
        }.also { layout ->
            checkAuth(UI.getCurrent(), VaadinRequest.getCurrent(), 50, layout, notLoggedIn = { noAccess() }) {
                h2("Welcome, ${it.name}!")
                init(initAdminView(it.permissions))
            }
        }
    }

    private fun initAdminView(perms: Int) = createComponent {
        accordion {
            setWidthFull()
            panel("Media Management") {
                init(MediaGrid(initialSearch = query))
            }
            panel("User Management") {
                init(userListing(perms < 99))
            }
            panel("Misc Utils") {
                button("Show unwatched shows") {
                    onLeftClick {
                        topNotification("Please wait.")
                        val ui = UI.getCurrent()
                        generateUnwatched(ui)
                    }
                }
            }
        }
    }

    override fun setParameter(event: BeforeEvent?, @OptionalParameter parameter: String?) {
        val params = event?.location?.queryParameters?.parameters
        if (params != null) {
            query = params["q"]?.firstOrNull()
        }
    }
}