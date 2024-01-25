package moe.styx.web.views

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setClassNames2
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.AnchorTarget
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.theme.lumo.LumoUtility.*
import moe.styx.types.User
import moe.styx.web.Main
import moe.styx.web.checkAuth
import moe.styx.web.components.authProgress
import moe.styx.web.components.linkButton
import moe.styx.web.components.noAccess
import moe.styx.web.createComponent
import moe.styx.web.layout.MainLayout
import moe.styx.web.unorderedList
import org.vaadin.lineawesome.LineAwesomeIcon

@PageTitle("Styx - User")
@Route("user", layout = MainLayout::class)
class UserView : KComposite() {

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
            h2("Welcome, ${user.name}!") { addClassNames(Padding.XSMALL) }
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
            linkButton("${Main.config.baseAPIURL}/logout", "Logout", LineAwesomeIcon.USER_ALT_SLASH_SOLID.create(), target = AnchorTarget.DEFAULT)
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