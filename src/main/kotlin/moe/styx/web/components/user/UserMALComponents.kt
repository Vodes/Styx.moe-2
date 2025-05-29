package moe.styx.web.components.user

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.AnchorTarget
import io.ktor.http.*
import kotlinx.coroutines.delay
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.data.User
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.common.extension.toBoolean
import moe.styx.common.util.launchThreaded
import moe.styx.db.tables.UserTable
import moe.styx.web.components.linkButton
import moe.styx.web.data.PKCEUtil
import moe.styx.web.dbClient
import moe.styx.web.topNotification
import kotlin.random.Random

class UserMALComponents(private val user: User) : KComposite() {
    val root = ui {
        verticalLayout(padding = false) {
            if (user.malData == null || user.malData!!.refreshTokenExpiry < currentUnixSeconds()) {
                h3(if (user.malData == null) "You don't have a MAL profile connected to Styx." else "Your MAL connection expired.")
                val current = UnifiedConfig.current
                val redirectURI = "${current.base.siteBaseURL()}/mal"
                val state = "MAL_AUTH_${this@UserMALComponents.user.name}_${Random.nextInt(55555)}"
                val verifier = PKCEUtil.generateVerifier(64)
                val malOAuthURL = URLBuilder("https://myanimelist.net/v1/oauth2/authorize").apply {
                    parameters.apply {
                        append("client_id", current.webConfig.malClientID)
                        append("state", state)
                        append("redirect_uri", redirectURI)
                        append("response_type", "code")
                        append("code_challenge", verifier)
                        append("code_challenge_method", "plain")
                    }
                }.buildString()
                PKCEUtil.generatedVerifiers[state] = verifier
                linkButton(malOAuthURL, "Connect MAL", target = AnchorTarget.DEFAULT)
                return@verticalLayout
            }
            div {
                html("MyAnimeList connected to <a href='https://myanimelist.net/profile/${user.malData!!.userName}' target='_blank'>${user.malData!!.userName}</a>.")
            }
            //nativeLabel("Keep in mind that Anilist connections only last a year and they don't provide a way for developers to automatically renew it.")
            button("Disconnect MAL") {
                onClick {
                    val ui = UI.getCurrent()
                    val success = dbClient.transaction {
                        UserTable.upsertItem(user.copy(malData = null))
                    }.insertedCount.toBoolean()
                    if (!success) {
                        topNotification("Failed to delete MAL association!")
                        return@onClick
                    }
                    topNotification("Successfully removed the MAL connection from your user!")
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
fun (@VaadinDsl HasComponents).userMalView(
    user: User,
    block: (@VaadinDsl UserMALComponents).() -> Unit = {}
) = init(
    UserMALComponents(user), block
)