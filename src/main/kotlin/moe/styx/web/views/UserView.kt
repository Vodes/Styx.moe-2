package moe.styx.web.views

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setClassNames2
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.AnchorTarget
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.theme.lumo.LumoUtility.Margin
import com.vaadin.flow.theme.lumo.LumoUtility.Padding
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.data.User
import moe.styx.common.data.WebTempLink
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.db.tables.WebTempLinkTable
import moe.styx.web.*
import moe.styx.web.components.authProgress
import moe.styx.web.components.linkButton
import moe.styx.web.components.noAccess
import moe.styx.web.components.user.*
import moe.styx.web.layout.MainLayout
import org.vaadin.addon.stefan.clipboard.ClientsideClipboard
import org.vaadin.lineawesome.LineAwesomeIcon
import java.security.SecureRandom
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@PageTitle("Styx - User")
@Route("user", layout = MainLayout::class)
class UserView : KComposite() {

    lateinit var tempButtonResultLayout: VerticalLayout
    lateinit var tempCreateButton: Button
    private val random = SecureRandom()
    private val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')

    val root = ui {
        verticalLayout(false) {
            setClassNames2(Margin.NONE, Padding.NONE)
            authProgress()
        }.also { layout ->
            checkAuth(UI.getCurrent(), VaadinRequest.getCurrent(), 0, layout, notLoggedIn = { noAccess() }) {
                init(userSettings(it))
            }
        }
    }

    private fun userSettings(user: User) = createComponent {
        verticalLayout(false) {
            setSizeFull()
            val config = UnifiedConfig.current
            h2("Welcome, ${user.name}!") { addClassNames(Padding.SMALL) }
            accordion {
                setWidthFull()
                panel("Downloads") {
                    isOpened = true
                    setClassNames2(Padding.SMALL)
                    content {
                        userDesktopDownloadButtons()
                        userAndroidDownloadButtons()
                        div {
                            setClassNames2(Padding.SMALL)
                            verticalLayout {
                                tempCreateButton = button("Create temporary download link") {
                                    onClick {
                                        val now = currentUnixSeconds()
                                        val expiry = now + (24.toDuration(DurationUnit.HOURS)).inWholeSeconds
                                        val tempURLSegment = (1..5).map {
                                            allowedChars[random.nextInt(0, allowedChars.size)]
                                        }.joinToString("")
                                        val success = dbClient.transaction {
                                            WebTempLinkTable.upsertItem(
                                                WebTempLink(tempURLSegment, now, expiry, user.GUID, "")
                                            ).insertedCount > 0
                                        }
                                        if (!success)
                                            topNotification("Failed to create temporary download link!")
                                        else {
                                            tempCreateButton.isEnabled = false
                                            addTempButtonResult("${UnifiedConfig.current.base.siteBaseURL()}/$tempURLSegment")
                                        }
                                    }
                                }
                            }
                            tempButtonResultLayout = verticalLayout {
                                isSpacing = false
                            }
                        }
                    }
                }
                panel("Devices") {
                    setClassNames2(Padding.SMALL)
                    content { deviceListView(user) }
                }
                if (config.webConfig.anilistClientID.isNotBlank() && config.webConfig.anilistClientSecret.isNotBlank())
                    panel("Anilist") {
                        setClassNames2(Padding.SMALL)
                        content { userAnilistView(user) }
                    }

                if (config.webConfig.malClientID.isNotBlank() && config.webConfig.malClientSecret.isNotBlank())
                    panel("MyAnimeList") {
                        setClassNames2(Padding.SMALL)
                        content { userMalView(user) }
                    }
            }
            linkButton(
                "${config.base.siteBaseURL()}/discord/logout",
                "Logout",
                LineAwesomeIcon.USER_ALT_SLASH_SOLID.create(),
                target = AnchorTarget.DEFAULT
            )
        }
    }

    private fun addTempButtonResult(tempURL: String) {
        tempButtonResultLayout.replaceAll {
            verticalLayout {
                isPadding = false
                h3("Temporary Download Link")
                horizontalLayout {
                    defaultVerticalComponentAlignment = FlexComponent.Alignment.END
                    textField("Templink") {
                        isEnabled = false
                        minWidth = "300px"
                        value = tempURL
                    }
                    button("Copy") {
                        onClick {
                            ClientsideClipboard.writeToClipboard(tempURL)
                        }
                    }
                }
                htmlSpan("This Link is only valid until the first time opened and only for 24 hours.<br>Make sure to not accidentally open it here if you want it elsewhere.")
                htmlSpan("<b>You can leave out the <i>'https://'</i> if you're typing this out manually.</b>")
            }
        }
    }
}
