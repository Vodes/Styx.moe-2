package moe.styx.web.views.sub

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.h2
import com.github.mvysny.karibudsl.v10.h3
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.AnchorTarget
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinRequest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.data.AnilistData
import moe.styx.common.data.User
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.common.extension.toBoolean
import moe.styx.common.http.httpClient
import moe.styx.common.json
import moe.styx.common.util.Log
import moe.styx.common.util.launchThreaded
import moe.styx.db.tables.UserTable
import moe.styx.downloader.utils.setGenericJsonBody
import moe.styx.web.checkAuth
import moe.styx.web.components.authProgress
import moe.styx.web.components.linkButton
import moe.styx.web.components.noAccess
import moe.styx.web.dbClient
import moe.styx.web.replaceAll
import pw.vodes.anilistkmp.AnilistApiClient
import pw.vodes.anilistkmp.ext.fetchViewer
import kotlin.jvm.optionals.getOrNull

@PageTitle("Styx - Anilist Registration")
@Route("anilist")
class AnilistCodeReceiveView : KComposite(), BeforeEnterObserver {
    private var code: String? = null
    private var attempt = 0
    private lateinit var currentLayout: VerticalLayout

    val root = ui {
        verticalLayout {
            authProgress()
        }.also { layout ->
            currentLayout = layout
            val ui = UI.getCurrent()
            checkAuth(ui, VaadinRequest.getCurrent(), 0, currentLayout, notLoggedIn = {
                noAccess()
            }) { user ->
                h2("Checking code...").also {
                    startChecking(ui, user)
                }
            }
        }
    }

    private fun startChecking(ui: UI, user: User) {
        launchThreaded {
            while (code == null && attempt < 5) {
                delay(1000)
                attempt++
            }
            if (code == null) {
                ui.access {
                    currentLayout.replaceAll {
                        h3("Could not fetch code from url. Please check back with the admin.")
                    }
                }
                return@launchThreaded
            }
            runCatching {
                val current = UnifiedConfig.current
                val redirectURI = "${current.base.siteBaseURL()}/anilist"
                val tokenRequestBody = GenericAuthBody(
                    "authorization_code",
                    current.webConfig.anilistClientID,
                    current.webConfig.anilistClientSecret,
                    redirectURI,
                    code!!
                )

                val response = httpClient.post("https://anilist.co/api/v2/oauth/token") {
                    setGenericJsonBody(tokenRequestBody)
                }
                val textBody = response.bodyAsText()
                if (!response.status.isSuccess()) {
                    ui.access {
                        currentLayout.replaceAll {
                            h3("Could not fetch authorization token. Please check back with the admin.")
                        }
                    }
                    Log.e { "Anilist token request failed with status ${response.status}.\nBody: $textBody" }
                    return@launchThreaded
                }
                val responseBody = json.decodeFromString<GenericTokenResponse>(textBody)
                val client = AnilistApiClient(responseBody.accessToken)
                val viewerResponse = client.fetchViewer()
                if (viewerResponse.data == null) {
                    ui.access {
                        currentLayout.replaceAll {
                            h3("Could not fetch user via received token. Please check back with the admin.")
                        }
                    }
                    Log.e { "Failed to fetch anilist viewer.\nException: ${viewerResponse.exception}\nErrors: ${viewerResponse.errors?.joinToString { it.toString() }}" }
                    return@launchThreaded
                }
                val anilistData = AnilistData(
                    responseBody.accessToken,
                    "",
                    currentUnixSeconds() + responseBody.expiresIn,
                    viewerResponse.data!!.name,
                    viewerResponse.data!!.id
                )
                val success = dbClient.transaction {
                    UserTable.upsertItem(user.copy(anilistData = anilistData))
                }.insertedCount.toBoolean()

                if (!success) {
                    ui.access {
                        currentLayout.replaceAll {
                            h3("Could not update anilist data for this user. Please check back with the admin.")
                        }
                    }
                    return@launchThreaded
                }
                ui.access {
                    currentLayout.replaceAll {
                        verticalLayout(padding = true) {
                            h3("Successfully authenticated anilist user.")
                            linkButton("${UnifiedConfig.current.base.siteBaseURL()}/user", "Return to user page", target = AnchorTarget.DEFAULT)
                        }
                    }
                }
            }.onFailure {
                ui.access {
                    currentLayout.replaceAll {
                        h3("Could not fetch authorization token. Please check back with the admin.")
                    }
                }
                Log.e(null, it) { "Failed to fetch authorization token from anilist" }
            }
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent?) {
        if (event == null)
            return
        event.location.queryParameters.getSingleParameter("code").getOrNull()?.let {
            this.code = it
        }
    }
}

@Serializable
private data class GenericAuthBody(
    @SerialName("grant_type")
    val grantType: String,
    @SerialName("client_id")
    val clientID: String,
    @SerialName("client_secret")
    val clientSecret: String,
    @SerialName("redirect_uri")
    val redirectURI: String,
    val code: String
)

@Serializable
data class GenericTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("expires_in")
    val expiresIn: Int,
    @SerialName("refresh_token")
    val refreshToken: String
)