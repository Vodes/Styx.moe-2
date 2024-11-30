package moe.styx.web.data

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.http.httpClient

@OptIn(ExperimentalSerializationApi::class)
val explicitNullJson = Json {
    explicitNulls = true
    encodeDefaults = true
}

@Serializable
data class DiscordContent(val content: String? = null, val embeds: List<DiscordEmbed> = emptyList())

@Serializable
data class DiscordEmbed(val title: String, val description: String, val thumbnail: DiscordThumbnail)

@Serializable
data class DiscordThumbnail(val url: String)

fun sendDiscordHookEmbed(title: String, description: String, imageURL: String) = runBlocking {
    val content = DiscordContent(
        embeds = listOf(
            DiscordEmbed(
                title, description, DiscordThumbnail(imageURL)
            )
        )
    )
    httpClient.post(UnifiedConfig.current.discord.announcementWebhookURL()) {
        contentType(ContentType.Application.Json)
        setBody(explicitNullJson.encodeToString(content))
    }
}