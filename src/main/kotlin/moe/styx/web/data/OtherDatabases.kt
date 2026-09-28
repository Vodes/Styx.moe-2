package moe.styx.web.data

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.styx.common.http.httpClient
import moe.styx.common.json
import org.jsoup.Jsoup

private var lastUpdated: Long = 0
private var currentDataset: List<MultiIDStorage> = listOf()

fun getMalIDForAnilistID(id: Int): Int? {
    val curTime = Clock.System.now().epochSeconds
    if ((curTime - 129600) > lastUpdated || currentDataset.isEmpty())
        updateDataset().also { lastUpdated = curTime }
    return currentDataset.findMalID(id)
}

fun getAnisearchIDForAnilistID(id: Int): Int? {
    val curTime = Clock.System.now().epochSeconds
    if ((curTime - 129600) > lastUpdated || currentDataset.isEmpty())
        updateDataset().also { lastUpdated = curTime }
    return currentDataset.findAnisearchID(id)
}

fun scrapeAnisearchDescription(id: Int): String? = runBlocking {
    val response = httpClient.get("https://www.anisearch.de/anime/$id") {
        headers {
            append(HttpHeaders.Referrer, "https://www.anisearch.de/anime/index/?char=all&text=&q=true")
            append(
                HttpHeaders.Accept,
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
            )
        }
        userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    }
    val doc = Jsoup.parse(response.bodyAsText(), "https://www.anisearch.de")
    val descriptionSection = doc.body().getElementById("description")
    val germanElements = descriptionSection?.getElementsByAttributeValue("lang", "de")
    val germanDescription = germanElements?.select("div.details-text")?.first() ?: return@runBlocking null
    return@runBlocking germanDescription.wholeText()
}

@Serializable
internal data class MultiIDStorage(
    @SerialName("anilist_id") val anilistID: Int? = null,
    @SerialName("mal_id") val malID: Int? = null,
    @SerialName("anisearch_id") val anisearchID: Int? = null
)

internal fun parseAnimeMappings(source: String): List<MultiIDStorage> =
    json.decodeFromString<List<MultiIDStorage>>(source).filter { it.anilistID != null }

internal fun List<MultiIDStorage>.findMalID(id: Int): Int? =
    firstOrNull { it.anilistID == id && it.malID != null }?.malID

internal fun List<MultiIDStorage>.findAnisearchID(id: Int): Int? =
    firstOrNull { it.anilistID == id && it.anisearchID != null }?.anisearchID

private fun updateDataset() = runBlocking {
    val response =
        httpClient.get("https://raw.githubusercontent.com/Fribb/anime-lists/master/anime-list-mini.json")
    if (response.status != HttpStatusCode.OK)
        return@runBlocking
    currentDataset = parseAnimeMappings(response.bodyAsText())
}
