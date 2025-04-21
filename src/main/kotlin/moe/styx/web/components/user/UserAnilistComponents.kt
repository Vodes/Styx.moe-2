package moe.styx.web.components.user

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.AnchorTarget
import kotlinx.coroutines.delay
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.data.User
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.common.extension.toBoolean
import moe.styx.common.util.launchThreaded
import moe.styx.db.tables.UserTable
import moe.styx.web.components.linkButton
import moe.styx.web.dbClient
import moe.styx.web.topNotification

class UserAnilistComponents(private val user: User) : KComposite() {
    val root = ui {
        verticalLayout(padding = false) {
            if (user.anilistData == null || user.anilistData!!.tokenExpiry < currentUnixSeconds()) {
                h3(if (user.anilistData == null) "You don't have an anilist profile connected to Styx." else "Your anilist connection expired.")
                val current = UnifiedConfig.current
                val redirectURI = "${current.base.siteBaseURL()}/anilist"
                val anilistOAuthUrl = "https://anilist.co/api/v2/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code"
                    .format(current.webConfig.anilistClientID, redirectURI)
                linkButton(anilistOAuthUrl, "Connect Anilist", target = AnchorTarget.DEFAULT)
                return@verticalLayout
            }
            div {
                html("Anilist connected to <a href='https://anilist.co/user/${user.anilistData!!.userID}' target='_blank'>${user.anilistData!!.userName}</a>.")
            }
            nativeLabel("Keep in mind that Anilist connections only last a year and they don't provide a way for developers to automatically renew it.")
            button("Disconnect Anilist") {
                onClick {
                    val ui = UI.getCurrent()
                    val success = dbClient.transaction {
                        UserTable.upsertItem(user.copy(anilistData = null))
                    }.insertedCount.toBoolean()
                    if (!success) {
                        topNotification("Failed to delete anilist association!")
                        return@onClick
                    }
                    topNotification("Successfully removed the anilist connection from your user!")
                    launchThreaded {
                        delay(2000)
                        ui.access {
                            ui.page.reload()
                        }
                    }
                }
            }
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).userAnilistView(
    user: User,
    block: (@VaadinDsl UserAnilistComponents).() -> Unit = {}
) = init(
    UserAnilistComponents(user), block
)