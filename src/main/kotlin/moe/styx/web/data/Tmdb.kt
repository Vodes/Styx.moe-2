package moe.styx.web.data

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.styx.types.json
import moe.styx.web.Main
import moe.styx.web.httpClient

@Serializable
data class ImageResults(
    val backdrops: List<TmdbImage>,
    val logos: List<TmdbImage>,
    val posters: List<TmdbImage>
)

@Serializable
data class TmdbImage(
    @SerialName("aspect_ratio")
    val aspectRatio: Double,
    val height: Int,
    val width: Int,
    @SerialName("iso_639_1")
    val languageCode: String?,
    @SerialName("file_path")
    val filePath: String,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("vote_count")
    val voteCount: Int
) {
    fun getURL() = "https://www.themoviedb.org/t/p/original$filePath"
}

@Serializable
data class TmdbMeta(
    val id: Int,
    val name: String,
    val overview: String
)

fun tmdbImageQuery(id: Int, tv: Boolean = true): ImageResults? = runBlocking {
    val response = httpClient.get("https://api.themoviedb.org/3/${if (tv) "tv" else "movie"}/$id/images") {
        accept(ContentType.Application.Json)
        bearerAuth(Main.config.tmdbToken)
        userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
    }

    if (response.status == HttpStatusCode.OK)
        return@runBlocking json.decodeFromString(response.bodyAsText())

    return@runBlocking null
}

fun getTmdbMetadata(id: Int, tv: Boolean = true, languageCode: String = "en-US", season: Int? = null): TmdbMeta? = runBlocking {
    var url = "https://api.themoviedb.org/3/${if (tv) "tv" else "movie"}/$id"
    if (tv && season != null) {
        url += "/season/$season"
    }
    val response = httpClient.get("$url?language=$languageCode") {
        accept(ContentType.Application.Json)
        bearerAuth(Main.config.tmdbToken)
        userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
    }

    if (response.status == HttpStatusCode.OK)
        return@runBlocking json.decodeFromString(response.bodyAsText())

    return@runBlocking null
}