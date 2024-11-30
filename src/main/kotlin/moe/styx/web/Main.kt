package moe.styx.web

import com.github.mvysny.vaadinboot.VaadinBoot
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.server.AppShellSettings
import com.vaadin.flow.shared.communication.PushMode
import com.vaadin.flow.shared.ui.Transport
import com.vaadin.flow.theme.Theme
import com.vaadin.flow.theme.lumo.Lumo
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.http.getHttpClient
import moe.styx.db.DBClient

@Theme("my-theme", variant = Lumo.DARK)
@Push(PushMode.AUTOMATIC, transport = Transport.WEBSOCKET_XHR)
class AppShell : AppShellConfigurator {
    override fun configurePage(settings: AppShellSettings?) {
        settings?.addLink("shortcut icon", "icons/icon.ico")
        settings?.addFavIcon("icon", "icons/icon.ico", "256x256")
        super.configurePage(settings)
    }
}

val dbClient by lazy {
    val config = UnifiedConfig.current.dbConfig
    DBClient(
        "jdbc:postgresql://${config.host()}/Styx",
        "org.postgresql.Driver",
        config.user(),
        config.pass(),
        10
    )
}

fun main(args: Array<String>) {
    getHttpClient("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    VaadinBoot().listenOn(UnifiedConfig.current.webConfig.serveHost()).setPort(UnifiedConfig.current.webConfig.servePort).run()
}
