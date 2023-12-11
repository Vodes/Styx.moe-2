package moe.styx.web

import io.ktor.client.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import kotlinx.serialization.json.Json
import moe.styx.types.json
import net.peanuuutz.tomlkt.Toml

val toml = Toml {
    ignoreUnknownKeys = true
    explicitNulls = true
}

val httpClient = HttpClient {
    install(ContentNegotiation) { json }
    install(ContentEncoding)
    install(HttpCookies)
}

val prettyPrintJson = Json(json) {
    prettyPrint = true
}