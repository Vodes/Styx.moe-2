package moe.styx.web.components

import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.init
import com.vaadin.flow.component.*
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.AnchorTarget
import com.vaadin.flow.shared.ApplicationConstants

class LinkButton : Composite<Anchor>(), HasTheme, HasStyle {
    companion object {
        fun withHref(href: String, text: String, icon: Component? = null, target: AnchorTarget = AnchorTarget.BLANK): LinkButton {
            icon?.let {
                return LinkButton().apply {
                    setHref(href)
                    setText(text)
                    setTarget(target)
                    setIcon(icon)
                }
            } ?: return LinkButton().apply {
                setHref(href)
                setText(text)
                setTarget(target)
            }
        }
    }

    private var button: Button = Button().apply {
        this.element.setAttribute("anchor", "")
    }

    override fun initContent(): Anchor {
        val anchor = super.initContent()
        anchor.add(button)
        return anchor
    }

    fun setRouterLink(value: Boolean) {
        if (value)
            element.setAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE, "")
        else
            element.removeAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE)
    }

    fun setHref(href: String) = content.setHref(href)
    fun setText(text: String) = button.setText(text)
    fun setIcon(icon: Component) = button.setIcon(icon)
    fun setTarget(target: AnchorTarget) = content.setTarget(target)
}

@VaadinDsl
fun (@VaadinDsl HasComponents).linkButton(
    href: String,
    text: String,
    icon: Component? = null,
    target: AnchorTarget = AnchorTarget.BLANK,
    block: (@VaadinDsl LinkButton).() -> Unit = {}
) = init(
    LinkButton.withHref(href, text, icon, target), block
)