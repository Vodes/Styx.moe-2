package moe.styx.web

import kotlinx.serialization.json.Json
import moe.styx.common.json
import net.peanuuutz.tomlkt.Toml

val toml = Toml {
    ignoreUnknownKeys = true
    explicitNulls = true
}

val prettyPrintJson = Json(json) {
    prettyPrint = true
}