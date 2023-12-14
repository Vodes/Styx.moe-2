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
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.server.VaadinRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.styx.db.getUsers
import moe.styx.web.auth.DiscordAPI
import moe.styx.web.views.HomeView

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

inline fun checkAuth(
    ui: UI,
    request: VaadinRequest?,
    minPerms: Int = 50,
    parent: FlexComponent,
    crossinline func: FlexComponent.() -> Component
) {
    CoroutineScope(Dispatchers.IO).launch {
        val discordUser = DiscordAPI.getUserFromToken(DiscordAPI.getCurrentToken(request) ?: "")
        if (discordUser == null) {
            ui.access { navigateTo<HomeView>() }
            return@launch
        }
        val user = getDBClient().executeGet { getUsers(mapOf("discordID" to discordUser.id)) }.find { it.permissions > minPerms }
        if (user == null) {
            ui.access { navigateTo<HomeView>() }
            return@launch
        }
        ui.access {
            parent.replaceAll { func(parent) }
        }
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