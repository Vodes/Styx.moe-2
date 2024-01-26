package moe.styx.web.data

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import moe.styx.types.json
import moe.styx.web.httpClient

val mediaProperties = """
            id
            title {
              romaji
              english
              native
            }
        	description(asHtml:false)
            startDate {
              year
              month
              day
            }
        	coverImage {
              extraLarge
            }
            bannerImage
            trailer {
              id
              site
            }
        	genres
            tags {
                name
                rank
                isMediaSpoiler
            }
""".trimIndent()

val mediaQuery = """
    query (${'$'}id: Int) {
      Media (id: ${'$'}id, type: ANIME) {
           $mediaProperties
      }
    }
""".trimIndent()

val searchQuery = """
    query (${'$'}page: Int, ${'$'}perPage: Int, ${'$'}search: String) {
        Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            pageInfo {
                hasNextPage
            }
            media(search: ${'$'}search, type: ANIME) {
                $mediaProperties
            }
        }
    }
""".trimIndent()

@Serializable
data class AniListMediaResult(
    val id: Int,
    val title: AniListTitle,
    @SerialName("description")
    private val _description: String?,
    val startDate: AniListDate? = null,
    val coverImage: AnilistCoverImage,
    val bannerImage: String? = null,
    val trailer: AniListTrailer? = null,
    val genres: List<String>,
    val tags: List<AnilistTag>
) {
    fun anyTitle(): String {
        if (title.english.isNullOrBlank())
            return if (title.romaji.isNullOrBlank()) title.native ?: "N/A" else title.romaji
        return title.english
    }

    fun listingURL() = "https://anilist.co/anime/$id"

    val description: String?
        get() = _description?.replace("<br><br>", "\n")?.replace("<br>", "")
}

@Serializable
data class AniListTitle(
    val english: String? = null,
    val romaji: String? = null,
    val native: String? = null
)

@Serializable
data class AniListDate(
    val year: Int? = -1,
    val month: Int? = -1,
    val day: Int? = -1
)

@Serializable
data class AniListTrailer(
    val id: String,
    val site: String
)

@Serializable
data class AnilistCoverImage(
    val extraLarge: String? = null,
    val large: String? = null
) {
    fun getURL() = extraLarge ?: large
}

@Serializable
data class AnilistTag(
    val name: String,
    val rank: Int,
    val isMediaSpoiler: Boolean
)

fun getAniListDataForID(id: Int): AniListMediaResult? = runBlocking {
    val contentMain = mapOf("query" to mediaQuery, "variables" to json.encodeToString(mapOf("id" to "$id")))

    val response = httpClient.post("https://graphql.anilist.co") {
        accept(ContentType.Application.Json)
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(contentMain))
    }
    if (response.status != HttpStatusCode.OK)
        return@runBlocking null

    val jsonObj: JsonObject = json.decodeFromString(response.bodyAsText())
    val dataObj = jsonObj["data"]?.jsonObject
    return@runBlocking dataObj?.let { json.decodeFromJsonElement(dataObj["Media"]!!) }
}

fun searchAniList(search: String): List<AniListMediaResult> = runBlocking {
    val results = listOf<AniListMediaResult>()
    val contentMain = mapOf("query" to searchQuery, "variables" to "{\"search\": \"$search\", \"page\": 0, \"perPage\": 50}")
    val response = httpClient.post("https://graphql.anilist.co") {
        accept(ContentType.Application.Json)
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(contentMain))
    }
    if (response.status != HttpStatusCode.OK)
        return@runBlocking results

    val jsonObj: JsonObject = json.decodeFromString(response.bodyAsText())
    val mediaArray = jsonObj["data"]?.jsonObject?.get("Page")?.jsonObject?.get("media")?.jsonArray ?: return@runBlocking results
    return@runBlocking mediaArray.map { json.decodeFromJsonElement(it) }
}