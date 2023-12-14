package moe.styx.web

import com.github.mvysny.kaributools.isEmpty
import com.vaadin.flow.router.QueryParameters
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.styx.types.*
import moe.styx.web.components.media.StackType
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

fun QueryParameters?.isEmptyOrNull() = this?.isEmpty ?: true

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

fun Media.getFirstIDFromMap(type: StackType): Int? {
    val mappingJson = metadataMap?.let {
        if (it.isBlank())
            return@let null
        json.decodeFromString<JsonObject>(it)
    } ?: return null
    val mapEntries = mappingJson[type.key]?.jsonObject?.entries ?: return null
    var value = mapEntries.firstOrNull()?.value?.jsonPrimitive?.content ?: return null
    if (value.contains("/"))
        value = value.split("/")[0]
    return (if (value.contains(",")) value.split(",")[0] else value).toIntOrNull()
}

fun Media.getFirstTMDBSeason(): Int? {
    val mappingJson = metadataMap?.let {
        if (it.isBlank())
            return@let null
        json.decodeFromString<JsonObject>(it)
    } ?: return null
    val mapEntries = mappingJson[StackType.TMDB.key]?.jsonObject?.entries ?: return null
    var value = mapEntries.firstOrNull()?.value?.jsonPrimitive?.content ?: return null
    if (value.contains("/"))
        value = value.split("/")[0]
    if (!value.contains(","))
        return null
    return value.split(",")[1].toIntOrNull()
}

fun isWindows() = System.getProperty("os.name").contains("win", true)