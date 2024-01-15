package moe.styx.web.data.tmdb

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import moe.styx.types.json
import moe.styx.web.Main
import moe.styx.web.httpClient

suspend inline fun <reified T> genericTmdbGet(url: String): T? {
    val response = httpClient.get(url) {
        accept(ContentType.Application.Json)
        bearerAuth(Main.config.tmdbToken)
        userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
    }

    if (response.status == HttpStatusCode.OK) {
        val text = response.bodyAsText()
        return json.decodeFromString<T>(text)
    }

    return null
}

fun tmdbImageQuery(id: Int, tv: Boolean = true): ImageResults? = runBlocking {
    return@runBlocking genericTmdbGet("https://api.themoviedb.org/3/${if (tv) "tv" else "movie"}/$id/images")
}

fun tmdbFindGroups(id: Int): TmdbGroupQuery? = runBlocking {
    return@runBlocking genericTmdbGet("https://api.themoviedb.org/3/tv/$id/episode_groups")
}

fun getTmdbMetadata(id: Int, tv: Boolean = true, languageCode: String = "en-US", season: Int? = null): TmdbMeta? = runBlocking {
    var url = "https://api.themoviedb.org/3/${if (tv) "tv" else "movie"}/$id"
    if (tv && season != null) {
        url += "/season/$season"
    }
    return@runBlocking genericTmdbGet("$url?language=$languageCode")
}

fun getTmdbSeason(id: Int, season: Int, languageCode: String = "en-US"): TmdbSeason? = runBlocking {
    return@runBlocking genericTmdbGet("https://api.themoviedb.org/3/tv/$id/season/$season?language=$languageCode")
}

fun getTmdbOrder(id: String): TmdbEpisodeOrder? = runBlocking {
    return@runBlocking genericTmdbGet("https://api.themoviedb.org/3/tv/episode_group/$id")
}