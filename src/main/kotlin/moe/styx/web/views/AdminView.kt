package moe.styx.web.views

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setClassNames2
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.*
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.common.data.User
import moe.styx.db.tables.UserTable
import moe.styx.web.*
import moe.styx.web.components.MediaGrid
import moe.styx.web.components.authProgress
import moe.styx.web.components.misc.categoryListing
import moe.styx.web.components.misc.generateUnwatched
import moe.styx.web.components.noAccess
import moe.styx.web.components.user.userListing
import moe.styx.web.layout.MainLayout
import org.jetbrains.exposed.sql.selectAll

@Route("admin", layout = MainLayout::class)
@PageTitle("Styx - Admin")
class AdminView : KComposite(), HasUrlParameter<String> {
    private var query: String? = null

    val root = ui {
        verticalLayout(false) {
            setClassNames2(LumoUtility.Margin.NONE, LumoUtility.Padding.NONE)
            authProgress()
        }.also { layout ->
            checkAuth(UI.getCurrent(), VaadinRequest.getCurrent(), 50, layout, notLoggedIn = {
                val hasUsers = dbClient.transaction { UserTable.query { selectAll().toList() }.toList().isNotEmpty() }
                if (hasUsers)
                    noAccess()
                else {
                    verticalLayout {
                        h2("There are no registered users just yet.\nPlease create an admin user now.")
                        val nameField = textField("Name") { minWidth = "400px" }
                        val discordIDField = textField("Discord-ID") { minWidth = "400px" }
                        button("Create") {
                            onClick {
                                if (nameField.value.isNullOrBlank()) {
                                    topNotification("Name is required.")
                                    return@onClick
                                }
                                if (discordIDField.value.isNullOrBlank()) {
                                    topNotification("A discord ID is required.")
                                    return@onClick
                                }
                                dbClient.transaction {
                                    UserTable.upsertItem(
                                        User(
                                            newGUID(),
                                            nameField.value,
                                            discordIDField.value,
                                            System.currentTimeMillis(),
                                            0L,
                                            99
                                        )
                                    )
                                }
                                UI.getCurrent().page.reload()
                            }
                        }
                    }
                }
            }) {
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
            panel("Category Management") {
                init(categoryListing(perms < 99))
            }
            panel("Misc Utils") {
                button("Show unwatched shows") {
                    onClick {
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