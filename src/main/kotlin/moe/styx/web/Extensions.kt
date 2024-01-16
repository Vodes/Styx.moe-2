package moe.styx.web

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import moe.styx.types.*
import moe.styx.web.components.media.StackType
import moe.styx.web.data.tmdb.TmdbEpisode
import moe.styx.web.data.tmdb.getTmdbOrder
import moe.styx.web.data.tmdb.getTmdbSeason
import java.io.File
import java.util.*

fun Long.toISODate(): String {
    val instant = Instant.fromEpochSeconds(this)
    val datetime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return datetime.formattedStr()
}

fun LocalDateTime.formattedStr(): String {
    return "${this.year}-${this.monthNumber.padString()}-${this.dayOfMonth.padString()} " +
            "${this.hour.padString()}:${this.minute.padString()}:${this.second.padString()}"
}

fun String.capitalize(): String = lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

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
        json.decodeFromString<MappingCollection>(it)
    } ?: return null
    return when (type.key) {
        "tmdb" -> mappingJson.tmdbMappings.minByOrNull { it.remoteID }?.remoteID
        "mal" -> mappingJson.malMappings.minByOrNull { it.remoteID }?.remoteID
        else -> mappingJson.anilistMappings.minByOrNull { it.remoteID }?.remoteID
    }
}

fun Media.getFirstTMDBSeason(): Int? {
    val mappingJson = metadataMap?.let {
        if (it.isBlank())
            return@let null
        json.decodeFromString<MappingCollection>(it)
    } ?: return null
    return mappingJson.tmdbMappings.minByOrNull { it.seasonEntry }?.seasonEntry
}

fun TMDBMapping.getRemoteEpisodes(language: String = "en-US", message: (content: String) -> Unit = {}): List<TmdbEpisode> {
    if (remoteID <= 0)
        message("No valid ID was found!").also { return emptyList() }

    if (orderType != null && !orderID.isNullOrBlank()) {
        val order = getTmdbOrder(orderID!!)
        if (order == null)
            message("No episode order was found!").also { return emptyList() }
        val group = order!!.groups.find { it.order == seasonEntry }
        if (group == null)
            message("Could not find season $seasonEntry in the episode group!").also { return emptyList() }
        return group!!.episodes
    }
    val season = getTmdbSeason(remoteID, seasonEntry, language)
    if (season == null)
        message("Could not get season $seasonEntry for $remoteID!").also { return emptyList() }

    return season!!.episodes
}

fun isWindows() = System.getProperty("os.name").contains("win", true)