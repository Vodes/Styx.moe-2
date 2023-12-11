package moe.styx.web.auth

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.styx.types.json
import moe.styx.web.httpClient

object DiscordAPI {
    fun getUserFromToken(token: String): DiscordUser? = runBlocking {
        if (token.isBlank())
            return@runBlocking null

        val response = httpClient.get("https://discord.com/api/users/@me") {
            accept(ContentType.Application.Json)
            bearerAuth(token)
            userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
        }

        if (response.status != HttpStatusCode.OK)
            return@runBlocking null
        return@runBlocking json.decodeFromString(response.bodyAsText())
    }
}

@Serializable
data class DiscordUser(
    val id: String,
    val username: String,
    @SerialName("global_name")
    val globalName: String
)
