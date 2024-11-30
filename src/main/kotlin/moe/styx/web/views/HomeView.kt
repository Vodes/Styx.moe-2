package moe.styx.web.views

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.h2
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteAlias
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.styx.web.layout.MainLayout

@PageTitle("Styx - Home")
@Route("", layout = MainLayout::class)
@RouteAlias("home")
class HomeView : KComposite() {
    val root = ui {
        verticalLayout {
            isPadding = false
            h2("No clue what to put here")
//            button("Woah") {
//                onClick {
//                    sendDiscordHookEmbed("Test Title", "Wew", "https://i.styx.moe/F3A34DF7-32F5-4AD6-8BEF-DBFB7D81E018.webp")
////                    doButtonThing(UI.getCurrent(), this)
//                }
//            }
        }
    }
}

private fun doButtonThing(ui: UI, button: Button) {
    CoroutineScope(Dispatchers.IO).launch {
//        val user = DiscordAPI.getUserFromToken(Main.config.debugToken)
//        ui.access {
//            if (user != null)
//                button.text = "Hello, ${user.username}!"
//            else
//                button.text = "You are not logged in."
//        }
    }
}
