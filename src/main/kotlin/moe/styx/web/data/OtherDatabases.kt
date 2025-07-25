package moe.styx.web.data

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
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
    return currentDataset.find { it.anilistID == id }?.malID
}

fun getAnisearchIDForAnilistID(id: Int): Int? {
    val curTime = Clock.System.now().epochSeconds
    if ((curTime - 129600) > lastUpdated || currentDataset.isEmpty())
        updateDataset().also { lastUpdated = curTime }
    return currentDataset.find { it.anilistID == id }?.anisearchID
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
private data class Data(val sources: List<String>, val title: String)

@Serializable
private data class Database(val data: List<Data>)

data class MultiIDStorage(val title: String, val anilistID: Int, val malID: Int, val anisearchID: Int?)

private fun updateDataset() = runBlocking {
    val response =
        httpClient.get("https://github.com/manami-project/anime-offline-database/releases/download/latest/anime-offline-database-minified.json")
    if (response.status != HttpStatusCode.OK)
        return@runBlocking
    val parsedDB = json.decodeFromString<Database>(response.bodyAsText())
    val numRegex = "\\D+".toRegex()
    currentDataset = parsedDB.data.map { data ->
        val anilistID = data.sources.find { it.contains("anilist.co") }?.replace(numRegex, "")?.toIntOrNull()
        val malID = data.sources.find { it.contains("myanimelist") }?.replace(numRegex, "")?.toIntOrNull()
        val anisearchID = data.sources.find { it.contains("anisearch.com") }?.replace(numRegex, "")?.toIntOrNull()
        if (anilistID == null || malID == null)
            return@map null
        return@map MultiIDStorage(data.title, anilistID, malID, anisearchID)
    }.filterNotNull()
}