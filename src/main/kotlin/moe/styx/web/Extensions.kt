package moe.styx.web

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import moe.styx.common.data.Image
import moe.styx.common.data.TMDBMapping
import moe.styx.common.data.tmdb.TmdbEpisode
import moe.styx.common.extension.formattedStr
import moe.styx.common.extension.toBoolean
import moe.styx.web.data.tmdb.getTmdbOrder
import moe.styx.web.data.tmdb.getTmdbSeason
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

fun TMDBMapping.getRemoteEpisodes(message: (content: String) -> Unit = {}): Pair<List<TmdbEpisode>, List<TmdbEpisode>> {
    val empty = emptyList<TmdbEpisode>() to emptyList<TmdbEpisode>()
    if (remoteID <= 0)
        message("No valid ID was found!").also { return empty }

    if (orderType != null && !orderID.isNullOrBlank()) {
        val order = getTmdbOrder(orderID!!)
        if (order == null)
            message("No episode order was found!").also { return empty }
        val group = order!!.groups.find { it.order == seasonEntry }
        if (group == null)
            message("Could not find season $seasonEntry in the episode group!").also { return empty }

        val otherOrder = getTmdbOrder(orderID!!, "de-DE")
        val otherGroup = otherOrder?.groups?.find { it.order == seasonEntry }
        return group!!.episodes to (otherGroup?.episodes ?: emptyList())
    }
    val season = getTmdbSeason(remoteID, seasonEntry, "en-US")
    if (season == null)
        message("Could not get season $seasonEntry for $remoteID!").also { return empty }

    val other = getTmdbSeason(remoteID, seasonEntry, "de-DE")
    if (other == null)
        message("Could not get season $seasonEntry for $remoteID!").also { return empty }

    return season!!.episodes to other!!.episodes
}