package moe.styx.web.data

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import moe.styx.types.json
import moe.styx.web.httpClient

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

@Serializable
private data class Data(val sources: List<String>, val title: String)

@Serializable
private data class Database(val data: List<Data>)

data class MultiIDStorage(val title: String, val anilistID: Int, val malID: Int, val anisearchID: Int?)

private fun updateDataset() = runBlocking {
    val response =
        httpClient.get("https://raw.githubusercontent.com/manami-project/anime-offline-database/master/anime-offline-database-minified.json")
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