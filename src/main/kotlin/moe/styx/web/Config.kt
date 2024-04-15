package moe.styx.web

import kotlinx.serialization.Serializable
import moe.styx.common.isWindows
import java.io.File

@Serializable
data class Config(
    val debugToken: String = "",
    val tmdbToken: String = "",
    val serveHost: String = "localhost",
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
    } else {
        val configDir = File(System.getProperty("user.home"), ".config")
        val mainDir = File(configDir, "Styx")
        val dir = File(mainDir, "Web")
        dir.mkdirs()
        dir
    }
}