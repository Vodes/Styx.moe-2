package moe.styx.web.components

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.Unit
import com.vaadin.flow.theme.lumo.LumoUtility

class AuthProgressBar : KComposite() {
    val root = ui {
        verticalLayout {
            isPadding = false
            isSpacing = false
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