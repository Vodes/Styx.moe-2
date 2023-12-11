package moe.styx.web

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import moe.styx.types.Image
import moe.styx.types.json
import moe.styx.types.padString
import moe.styx.types.toBoolean
import java.io.File

fun Long.toISODate(): String {
    val instant = Instant.fromEpochSeconds(this)
    val datetime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return datetime.formattedStr()
}

fun LocalDateTime.formattedStr(): String {
    return "${this.year}-${this.monthNumber.padString()}-${this.dayOfMonth.padString()} " +
            "${this.hour.padString()}:${this.minute.padString()}:${this.second.padString()}"
}

fun Image.getURL(): String {
    return if (hasWEBP?.toBoolean() == true) {
        "https://i.styx.moe/$GUID.webp"
    } else if (hasJPG?.toBoolean() == true) {
        "https://i.styx.moe/$GUID.jpg"
    } else if (hasPNG?.toBoolean() == true) {
        "https://i.styx.moe/$GUID.png"
    } else {
        return externalURL as String
    }
}

fun Image.deleteIfExists() {
    val url = getURL()
    if (!url.contains("styx.moe"))
        return
    val file = File(Main.config.imageDir, url.split("/").last())
    if (file.exists())
        file.delete()
}

fun isWindows() = System.getProperty("os.name").contains("win", true)

val prettyPrintJson = Json(json) {
    prettyPrint = true
}