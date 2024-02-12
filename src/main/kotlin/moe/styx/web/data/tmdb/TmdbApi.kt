package moe.styx.web.data.tmdb

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import moe.styx.common.http.httpClient
import moe.styx.common.json
import moe.styx.web.Main

suspend inline fun <reified T> genericTmdbGet(url: String): T? {
    val response = httpClient.get(url) {
        accept(ContentType.Application.Json)
        bearerAuth(Main.config.tmdbToken)
        userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
    }

    if (response.status == HttpStatusCode.OK)
        return json.decodeFromString<T>(response.bodyAsText())

    return null
}

fun tmdbImageQuery(id: Int, tv: Boolean = true): ImageResults? = runBlocking {
    return@runBlocking genericTmdbGet("https://api.themoviedb.org/3/${if (tv) "tv" else "movie"}/$id/images")
}

fun tmdbFindGroups(id: Int): TmdbGroupQuery? = runBlocking {
    return@runBlocking genericTmdbGet("https://api.themoviedb.org/3/tv/$id/episode_groups")
}

fun tmdbFindMedia(search: String, tv: Boolean = true): List<TmdbMeta> = runBlocking {
    val urlBuilder = URLBuilder("https://api.themoviedb.org/3/search/${if (tv) "tv" else "movie"}")
    urlBuilder.parameters.apply {
        append("query", search)
        append("include_adult", "true")
        append("page", "1")
    }
    val response = httpClient.get(urlBuilder.buildString()) {
        accept(ContentType.Application.Json)
        bearerAuth(Main.config.tmdbToken)
        userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
    }
    val list = listOf<TmdbMeta>()
    if (response.status != HttpStatusCode.OK)
        return@runBlocking list

    val jsonObj = json.decodeFromString<JsonObject>(response.bodyAsText())
    val results = jsonObj["results"]?.jsonArray ?: return@runBlocking list
    return@runBlocking json.decodeFromJsonElement(results)
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

fun getTmdbOrder(id: String, languageCode: String = "en-US"): TmdbEpisodeOrder? = runBlocking {
    return@runBlocking genericTmdbGet("https://api.themoviedb.org/3/tv/episode_group/$id?language=$languageCode")
}