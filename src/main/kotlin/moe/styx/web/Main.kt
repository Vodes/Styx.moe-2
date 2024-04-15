package moe.styx.web

import com.github.mvysny.vaadinboot.VaadinBoot
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.server.AppShellSettings
import com.vaadin.flow.shared.communication.PushMode
import com.vaadin.flow.shared.ui.Transport
import com.vaadin.flow.theme.Theme
import com.vaadin.flow.theme.lumo.Lumo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import moe.styx.common.http.getHttpClient
import moe.styx.db.DBClient
import moe.styx.downloader.loadConfig
import net.peanuuutz.tomlkt.Toml
import java.io.File
import kotlin.system.exitProcess

@Theme("my-theme", variant = Lumo.DARK)
@Push(PushMode.AUTOMATIC, transport = Transport.WEBSOCKET_XHR)
class AppShell : AppShellConfigurator {
    override fun configurePage(settings: AppShellSettings?) {
        settings?.addLink("shortcut icon", "icons/icon.ico")
        settings?.addFavIcon("icon", "icons/icon.ico", "256x256")
        super.configurePage(settings)
    }
}

val toml = Toml {
    ignoreUnknownKeys = true
    explicitNulls = true
}

object Main {
    lateinit var appDir: File
    lateinit var configFile: File
    lateinit var config: Config
    lateinit var changesFile: File
}

val dbClient by lazy {
    DBClient(
        "jdbc:postgresql://${Main.config.dbConfig.ip}/Styx",
        "org.postgresql.Driver",
        Main.config.dbConfig.user,
        Main.config.dbConfig.pass,
        20
    )
}

fun main(args: Array<String>) {
    loadConfig()
    getHttpClient("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    Main.appDir = if (args.isEmpty()) getAppDir() else File(args[0]).also { it.mkdirs() }
    Main.changesFile = File(Main.appDir.parentFile, "changes.json")
    Main.configFile = File(Main.appDir, "config.toml")
    if (!Main.configFile.exists()) {
        Main.configFile.writeText(toml.encodeToString(Config()))
        println("Please setup your config at: ${Main.configFile.absolutePath}")
        exitProcess(1)
    }
    Main.config = toml.decodeFromString(Main.configFile.readText())
    VaadinBoot().listenOn(Main.config.serveHost).setPort(Main.config.servePort).run()
}
