package moe.styx.web.auth

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.http.httpClient
import moe.styx.common.json

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

    fun exchangeCode(code: String): GenericTokenResponse? = runBlocking {
        if (code.isBlank())
            return@runBlocking null

        val response = httpClient.submitForm("https://discord.com/api/oauth2/token", formParameters = parameters {
            append("grant_type", "authorization_code")
            append("client_id", UnifiedConfig.current.discord.discordClientID())
            append("client_secret", UnifiedConfig.current.discord.discordClientSecret())
            append("redirect_uri", "${UnifiedConfig.current.base.siteBaseURL()}/discord/auth")
            append("code", code)
        }) {
            method = HttpMethod.Post
            contentType(ContentType.Application.FormUrlEncoded)
            accept(ContentType.Application.Json)
        }

        if (response.status != HttpStatusCode.OK)
            return@runBlocking null
        return@runBlocking json.decodeFromString(response.bodyAsText())
    }

    fun buildAuthURL(state: String? = null): String {
        val builder = URLBuilder("https://discord.com/api/oauth2/authorize")
        builder.parameters.append("client_id", UnifiedConfig.current.discord.discordClientID())
        builder.parameters.append("redirect_uri", "${UnifiedConfig.current.base.siteBaseURL()}/discord/auth")
        builder.parameters.append("response_type", "code")
        builder.parameters.append("scope", "identify guilds")
        if (!state.isNullOrBlank())
            builder.parameters.append("state", state)
        return builder.buildString()
    }
}

@Serializable
data class DiscordUser(
    val id: String,
    val username: String,
    @SerialName("global_name")
    val globalName: String
)
