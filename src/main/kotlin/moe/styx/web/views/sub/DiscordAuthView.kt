package moe.styx.web.views.sub

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.h3
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.component.html.AnchorTarget
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinSession
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.data.WebLogin
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.db.tables.UserTable
import moe.styx.db.tables.WebLoginTable
import moe.styx.web.auth.AuthCookie
import moe.styx.web.auth.DiscordAPI
import moe.styx.web.components.authProgress
import moe.styx.web.components.linkButton
import moe.styx.web.dbClient
import moe.styx.web.replaceAll
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.uuid.ExperimentalUuidApi

private const val DISCORD_STATE_KEY = "discord_oauth_state"

@PageTitle("Styx - Discord Login")
@Route("discord/auth")
class DiscordAuthView : KComposite(), BeforeEnterObserver {
    private lateinit var currentLayout: VerticalLayout

    val root = ui {
        verticalLayout {
            authProgress()
        }.also {
            currentLayout = it
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun beforeEnter(event: BeforeEnterEvent?) {
        if (event == null)
            return

        val config = UnifiedConfig.current
        if (config.discord.discordClientID().isBlank() || config.discord.discordClientSecret().isBlank()) {
            showError("Discord authentication is not configured on this server.")
            return
        }

        val error = event.location.queryParameters.getSingleParameter("error").getOrNull()
        if (!error.isNullOrBlank()) {
            showError("Discord rejected the login request: $error")
            return
        }

        val code = event.location.queryParameters.getSingleParameter("code").getOrNull()
        if (code.isNullOrBlank()) {
            val state = UUID.randomUUID().toString()
            VaadinSession.getCurrent().setAttribute(DISCORD_STATE_KEY, state)
            event.ui.page.setLocation(DiscordAPI.buildAuthURL(state))
            return
        }

        val state = event.location.queryParameters.getSingleParameter("state").getOrNull()
        val expectedState = VaadinSession.getCurrent().getAttribute(DISCORD_STATE_KEY) as String?
        VaadinSession.getCurrent().setAttribute(DISCORD_STATE_KEY, null)
        if (state.isNullOrBlank() || expectedState.isNullOrBlank() || state != expectedState) {
            showError("Discord login state was invalid or expired. Please try again.")
            return
        }

        val tokenResponse = runCatching { DiscordAPI.exchangeCode(code) }.getOrNull()
        if (tokenResponse == null) {
            showError("Could not exchange the Discord authorization code.")
            return
        }

        val discordUser = DiscordAPI.getUserFromToken(tokenResponse.accessToken)
        if (discordUser == null) {
            showError("Could not get discord user from token!")
            return
        }

        val matchingStyxUser = dbClient.transaction {
            UserTable.query { selectAll().where { discordID eq discordUser.id }.toList() }
        }.firstOrNull()
        if (matchingStyxUser == null) {
            showError("This discord user is not registered on Styx!")
            return
        }

        val newAccessToken = UUID.randomUUID().toString()
        val now = currentUnixSeconds()
        val expiry = now + (30 * 3).toDuration(DurationUnit.DAYS).inWholeSeconds // 3 Months
        val result = dbClient.transaction {
            WebLoginTable.upsertItem(WebLogin(matchingStyxUser.GUID, now, expiry, newAccessToken)).insertedCount > 0
        }
        if (!result) {
            showError("Failed to create new login token and adding it to the database.")
            return
        }

        AuthCookie.setCurrentToken(newAccessToken, (expiry - now).toInt())
        event.ui.page.setLocation("${config.base.siteBaseURL()}/user")
    }

    private fun showError(text: String) {
        currentLayout.replaceAll {
            verticalLayout {
                h3(text)
                linkButton(
                    "${UnifiedConfig.current.base.siteBaseURL()}/discord/auth",
                    "Try again",
                    target = AnchorTarget.DEFAULT
                )
            }
        }
    }
}

@PageTitle("Styx - Discord Logout")
@Route("discord/logout")
class DiscordLogoutView : KComposite(), BeforeEnterObserver {
    val root = ui {
        verticalLayout {
            authProgress()
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent?) {
        if (event == null)
            return

        val current = AuthCookie.getCurrentToken(VaadinRequest.getCurrent())
        if (!current.isNullOrBlank()) {
            transaction {
                WebLoginTable.deleteWhere { token eq current }
            }
        }
        AuthCookie.clearCurrentToken()

        event.ui.page.setLocation(UnifiedConfig.current.base.siteBaseURL())
    }
}
