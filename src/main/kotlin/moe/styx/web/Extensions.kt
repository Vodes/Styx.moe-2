package moe.styx.web

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import moe.styx.common.data.Image
import moe.styx.common.data.MappingCollection
import moe.styx.common.data.Media
import moe.styx.common.data.TMDBMapping
import moe.styx.common.extension.formattedStr
import moe.styx.common.extension.toBoolean
import moe.styx.common.json
import moe.styx.web.components.media.StackType
import moe.styx.web.data.tmdb.TmdbEpisode
import moe.styx.web.data.tmdb.getTmdbOrder
import moe.styx.web.data.tmdb.getTmdbSeason
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.math.floor

fun Long.toISODate(): String {
    val instant = Instant.fromEpochSeconds(this)
    val datetime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return datetime.formattedStr()
}

private val small = DecimalFormat("#.#")
private val big = DecimalFormat("#.##")

fun Long.readableSize(useBinary: Boolean = false): String {
    val units = if (useBinary) listOf("B", "KiB", "MiB", "GiB", "TiB") else listOf("B", "KB", "MB", "GB", "TB")
    val divisor = if (useBinary) 1024 else 1000
    var steps = 0
    var current = this.toDouble()
    while (floor((current / divisor)) > 0) {
        current = (current / divisor)
        steps++;
    }
    small.roundingMode = RoundingMode.CEILING.also { big.roundingMode = it }
    return "${(if (steps > 2) big else small).format(current)} ${units[steps]}"
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