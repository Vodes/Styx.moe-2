package moe.styx.web

import kotlinx.serialization.Serializable
import moe.styx.common.isWindows
import net.peanuuutz.tomlkt.TomlComment
import java.io.File
import moe.styx.downloader.Main as DownloaderMain

@Serializable
data class Config(
    val debugToken: String = "",
    val tmdbToken: String = "",
    val serveHost: String = if (isDocker) "0.0.0.0" else "localhost",
    @TomlComment("Would not recommend changing the port if you're running this in docker.")
    val servePort: Int = 8080,
    val baseURL: String = "https://example.com",
    val baseAPIURL: String = "",
    val imageURL: String = "",
    val imageDir: String = "",
    val buildDir: String = "",
    val androidBuildDir: String = "",
    val webhookURL: String = "",
    val dbConfig: DbConfig = DbConfig()
)

@Serializable
data class DbConfig(val ip: String = "", val user: String = "", val pass: String = "")

fun getAppDir(): File {
    return if (isWindows) {
        val mainDir = File(System.getenv("APPDATA"), "Styx")
        val dir = File(mainDir, "Web")
        dir.mkdirs()
        dir
    } else if (isDocker) {
        DownloaderMain.appDir = File("/dl-config").also { it.mkdirs() }
        DownloaderMain.configFile = File(DownloaderMain.appDir, "config.toml")
        File("/config").also { it.mkdirs() }
    } else {
        val configDir = File(System.getProperty("user.home"), ".config")
        val mainDir = File(configDir, "Styx")
        val dir = File(mainDir, "Web")
        dir.mkdirs()
        dir
    }
}