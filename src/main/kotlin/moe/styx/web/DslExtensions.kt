package moe.styx.web

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.init
import com.github.mvysny.kaributools.navigateTo
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.UnorderedList
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.server.VaadinRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.styx.common.data.User
import moe.styx.db.tables.UserTable
import moe.styx.web.auth.DiscordAPI
import moe.styx.web.views.HomeView
import org.jetbrains.exposed.sql.selectAll

fun createComponent(block: HasComponents.() -> Component): KComposite {
    class Comp : KComposite() {
        val root = ui {
            block()
        }
    }
    return Comp()
}

fun FlexComponent.replaceAll(block: HasComponents.() -> Component) {
    removeAll()
    add(block())
}

fun topNotification(text: String, millis: Int = 1200): Notification {
    return Notification.show(text, millis, Notification.Position.TOP_CENTER).also { it.open() }
}

inline fun checkAuth(
    ui: UI,
    request: VaadinRequest?,
    minPerms: Int = 50,
    parent: FlexComponent? = null,
    noinline notLoggedIn: (FlexComponent.() -> Component)? = null,
    crossinline func: FlexComponent.(User) -> Component
) {
    CoroutineScope(Dispatchers.IO).launch {
        val discordUser = DiscordAPI.getUserFromToken(DiscordAPI.getCurrentToken(request) ?: "")
        if (discordUser == null) {
            if (notLoggedIn != null)
                parent?.let { ui.access { it.replaceAll { init(notLoggedIn(it)) } } }
            else
                ui.access { navigateTo<HomeView>() }
            return@launch
        }
        val user = dbClient.transaction { UserTable.query { selectAll().where { discordID eq discordUser.id }.toList() } }
            .find { it.permissions >= minPerms }
        if (user == null) {
            if (notLoggedIn != null)
                parent?.let { ui.access { it.replaceAll { init(notLoggedIn(it)) } } }
            else
                ui.access { navigateTo<HomeView>() }
            return@launch
        }
        parent?.let { ui.access { it.replaceAll { init(func(it, user)) } } }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).unorderedList(
    block: (@VaadinDsl UnorderedList).() -> Unit = {}
) = init(
    UnorderedList(), block
)

@VaadinDsl
fun (@VaadinDsl HasComponents).nav(
    block: (@VaadinDsl Nav).() -> Unit = {}
) = init(
    Nav(), block
)