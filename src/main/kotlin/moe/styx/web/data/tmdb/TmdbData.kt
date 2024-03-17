package moe.styx.web.data.tmdb

import moe.styx.common.data.tmdb.TmdbEpisode
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit

private val format = SimpleDateFormat("yyyy-MM-dd")

fun TmdbEpisode.parseDate() = format.parse(airDate)
fun TmdbEpisode.parseDateUnix() = parseDate().toInstant().plus(12, ChronoUnit.HOURS).epochSecond