package moe.styx.web

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import moe.styx.common.data.Image
import moe.styx.common.extension.formattedStr
import moe.styx.common.extension.toBoolean
import java.io.File
import java.util.*

fun Long.toISODate(): String {
    val instant = Instant.fromEpochSeconds(this)
    val datetime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return datetime.formattedStr()
}

fun newGUID() = UUID.randomUUID().toString().uppercase()

fun Image.getURL(): String {
    return if (hasWEBP?.toBoolean() == true) {
        "${Main.config.imageURL}/$GUID.webp"
    } else if (hasJPG?.toBoolean() == true) {
        "${Main.config.imageURL}/$GUID.jpg"
    } else if (hasPNG?.toBoolean() == true) {
        "${Main.config.imageURL}/$GUID.png"
    } else {
        return externalURL as String
    }
}

fun Image.deleteIfExists() {
    val url = getURL()
    if (!url.contains(Main.config.imageURL, true))
        return
    val file = File(Main.config.imageDir, url.split("/").last())
    if (file.exists())
        file.delete()
}