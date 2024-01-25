package moe.styx.web.components

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.Unit
import com.vaadin.flow.component.html.AnchorTarget
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.web.Main
import org.vaadin.lineawesome.LineAwesomeIcon

class AuthProgressBar : KComposite() {
    val root = ui {
        verticalLayout(false) {
            nativeLabel("Checking login...") { addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.NONE) }
            progressBar(indeterminate = true) {
                addClassNames(LumoUtility.Padding.XSMALL, LumoUtility.Margin.NONE)
                setWidth(4F, Unit.REM)
            }
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).authProgress() = init(AuthProgressBar())

class NoAccessComp : KComposite() {
    val root = ui {
        verticalLayout(false) {
            h3("You are not logged in or don't have sufficient permissions.")
            linkButton("${Main.config.baseAPIURL}/auth", "Login", LineAwesomeIcon.USER_LOCK_SOLID.create(), target = AnchorTarget.DEFAULT)
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).noAccess() = init(NoAccessComp())