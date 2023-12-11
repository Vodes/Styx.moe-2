package moe.styx.web

import com.github.mvysny.vaadinboot.VaadinBoot
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.shared.communication.PushMode
import com.vaadin.flow.theme.Theme
import com.vaadin.flow.theme.lumo.Lumo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import moe.styx.db.StyxDBClient
import java.io.File
import kotlin.system.exitProcess

@Theme("my-theme", variant = Lumo.DARK)
@Push(PushMode.AUTOMATIC)
class AppShell : AppShellConfigurator

object Main {
    lateinit var appDir: File
    lateinit var configFile: File
    lateinit var config: Config
}

fun getDBClient(): StyxDBClient {
    return StyxDBClient(
        "com.mysql.cj.jdbc.Driver",
        "jdbc:mysql://${Main.config.dbConfig.ip}/Styx2?" +
                "useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Europe/Berlin",
        Main.config.dbConfig.user,
        Main.config.dbConfig.pass
    )
}

inline fun openDB(func: StyxDBClient.() -> Unit) {
    val dbClient = getDBClient()
    dbClient.executeAndClose(func)
}

fun main(args: Array<String>) {
    Main.appDir = if (args.isEmpty()) getAppDir() else File(args[0]).also { it.mkdirs() }
    Main.configFile = File(Main.appDir, "config.toml")
    if (!Main.configFile.exists()) {
        Main.configFile.writeText(toml.encodeToString(Config()))
        println("Please setup your config at: ${Main.configFile.absolutePath}")
        exitProcess(1)
    }
    Main.config = toml.decodeFromString(Main.configFile.readText())
    VaadinBoot().run()
}
