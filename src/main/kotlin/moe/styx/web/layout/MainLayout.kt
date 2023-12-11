package moe.styx.web.layout

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasElement
import com.vaadin.flow.component.html.Header
import com.vaadin.flow.router.HighlightConditions
import com.vaadin.flow.router.RouterLayout
import com.vaadin.flow.theme.lumo.LumoUtility.*
import moe.styx.web.nav
import moe.styx.web.unorderedList
import moe.styx.web.views.AdminView
import moe.styx.web.views.HomeView
import moe.styx.web.views.UserView
import org.vaadin.lineawesome.LineAwesomeIcon

class MainLayout : KComposite(), RouterLayout {
    private val root = ui {
        verticalLayout {
            setSizeFull()
            content { align(stretch, top) }
            val header = Header()
            header.addClassNames(BoxSizing.BORDER, Display.FLEX, FlexDirection.COLUMN, Width.FULL)

            val layout = div {
                addClassNames(Display.FLEX, AlignItems.CENTER, Padding.Horizontal.LARGE)
                routerLink(viewType = HomeView::class) {
                    highlightCondition = HighlightConditions.never()
                    h1("Styx") { addClassNames(Margin.Vertical.MEDIUM, Margin.End.LARGE, FontSize.XLARGE) }
                }

                val linkClasses = arrayOf(Display.FLEX, Gap.XSMALL, Height.MEDIUM, AlignItems.CENTER, Padding.Horizontal.SMALL, TextColor.BODY)
                val spanClasses = arrayOf(FontWeight.MEDIUM, FontSize.MEDIUM, Whitespace.NOWRAP)

                nav {
                    addClassNames(Display.FLEX, Overflow.AUTO, Padding.Horizontal.MEDIUM, Padding.Vertical.XSMALL, Margin.End.AUTO)
                    unorderedList {
                        addClassNames(Display.FLEX, Gap.SMALL, ListStyleType.NONE, Margin.NONE, Padding.NONE)
                        routerLink(viewType = UserView::class) {
                            addClassNames(*linkClasses)
                            add(LineAwesomeIcon.USER_SOLID.create())
                            span("User") { addClassNames(*spanClasses) }
                        }
                        routerLink(viewType = AdminView::class) {
                            addClassNames(*linkClasses)
                            add(LineAwesomeIcon.USER_SECRET_SOLID.create())
                            span("Admin") { addClassNames(*spanClasses) }
                        }
                    }
                }
            }
            header.add(layout)
            add(header)

        }
    }

    override fun showRouterLayoutContent(content: HasElement?) {
        root.add(content as Component)
        content.isExpand = true
    }
}