package moe.styx.web.views

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setClassNames2
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.theme.lumo.LumoUtility.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.styx.web.Main
import moe.styx.web.auth.DiscordAPI
import moe.styx.web.auth.DiscordUser
import moe.styx.web.components.authProgress
import moe.styx.web.components.linkButton
import moe.styx.web.createComponent
import moe.styx.web.layout.MainLayout
import moe.styx.web.replaceAll
import moe.styx.web.unorderedList
import org.vaadin.lineawesome.LineAwesomeIcon

@PageTitle("Styx - User")
@Route("user", layout = MainLayout::class)
class UserView : KComposite() {

    private lateinit var layout: VerticalLayout

    val root = ui {
        verticalLayout {
            setClassNames2(Margin.NONE, Padding.NONE)
            isSpacing = false
            isPadding = false
            layout = verticalLayout {
                authProgress()
            }
        }.also {
            initUI(UI.getCurrent())
        }
    }

    private fun initUI(ui: UI) {
        CoroutineScope(Dispatchers.IO).launch {
            val user = DiscordAPI.getUserFromToken(Main.config.debugToken)
            ui.access {
                if (user != null)
                    layout.replaceAll { userSettings(user) }
                else
                    layout.replaceAll { h2("You're not logged in.") }
            }
        }
    }

    private fun userSettings(user: DiscordUser) = createComponent {
        verticalLayout {
            isSpacing = false
            setClassNames2(Padding.NONE)
            setSizeFull()
            h2("Welcome, ${user.username}!") { addClassNames(Padding.XSMALL) }
            accordion {
                setWidthFull()
                panel("Downloads") {
                    isOpened = true
                    setClassNames2(Padding.XSMALL)
                    content { add(downloadButtons()) }
                }
                panel("Devices") {
                    setClassNames2(Padding.XSMALL)
                    content { h2("Nothing here yet.") }
                }
            }
        }
    }

    private fun downloadButtons() = createComponent {
        horizontalLayout {
            setClassNames2(Padding.Horizontal.SMALL)
            unorderedList {
                addClassNames(Display.FLEX, Gap.SMALL, ListStyleType.NONE, Margin.NONE, Padding.NONE)
                linkButton(
                    "https://vodes.pw/awcp/Application%20Versions/UpdaterLinks/Styx%20Launcher%20v5.exe",
                    "Windows",
                    LineAwesomeIcon.WINDOWS.create()
                )
                linkButton(
                    "https://vodes.pw/awcp/Application%20Versions/UpdaterLinks/Styx%20Launcher%20v5.jar",
                    "Other (.jar)",
                    LineAwesomeIcon.DESKTOP_SOLID.create()
                )
                linkButton(
                    "https://vodes.pw/awcp/Application%20Versions/Android/pw.vodes.styx-0.1.3.apk",
                    "Android",
                    LineAwesomeIcon.ANDROID.create()
                )
            }
        }
    }
}