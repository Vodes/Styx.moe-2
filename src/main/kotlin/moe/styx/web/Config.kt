package moe.styx.web

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class Config(
    val debugToken: String = "",
    val tmdbToken: String = "",
    val serveHost: String = "localhost",
    val servePort: Int = 8080,
    val dbConfig: DbConfig = DbConfig()
)

@Serializable
data class DbConfig(val ip: String = "", val user: String = "", val pass: String = "")

fun getAppDir(): File {
    return if (System.getProperty("os.name").lowercase().contains("win")) {
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