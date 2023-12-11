package moe.styx.web.views

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.styx.db.getUsers
import moe.styx.types.User
import moe.styx.web.Main
import moe.styx.web.auth.DiscordAPI
import moe.styx.web.components.authProgress
import moe.styx.web.components.initMediaComponent
import moe.styx.web.createComponent
import moe.styx.web.getDBClient
import moe.styx.web.layout.MainLayout
import moe.styx.web.replaceAll

@Route("admin", layout = MainLayout::class)
@PageTitle("Styx - Admin")
class AdminView : KComposite() {
    private lateinit var layout: VerticalLayout
    private lateinit var uiScope: UI

    val root = ui {
        uiScope = UI.getCurrent()
        verticalLayout {
            isSpacing = false
            isPadding = false
            layout = verticalLayout {
                authProgress()
            }
        }
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val discordUser = DiscordAPI.getUserFromToken(Main.config.debugToken)
            if (discordUser != null) {
                val users: List<User> = getDBClient().executeGet { getUsers(mapOf("discordID" to discordUser.id, "permissions" to 99)) }
                uiScope.access {
                    if (users.isEmpty())
                        layout.replaceAll { h2("You are not in the Styx database or not an admin.") }
                    else {
                        val user = users.first()
                        layout.replaceAll { h2("Welcome, ${user.name}!") }
                        layout.add(initAdminView())
                    }
                }
            } else
                uiScope.access { layout.replaceAll { h2("You're not logged in.") } }
        }
    }

    private fun initAdminView() = createComponent {
        val dbClient = getDBClient()
        accordion {
            setWidthFull()
            panel("Media Management") {
                add(initMediaComponent(dbClient))
            }
            panel("User Management") {
                h2("Test 2")
            }
        }.also {
            dbClient.closeConnection()
        }
    }
}