package moe.styx.web.components

import com.github.mvysny.karibudsl.v10.contextMenu
import com.github.mvysny.karibudsl.v10.item
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.textfield.TextField
import moe.styx.common.data.Media
import moe.styx.common.data.TokenGroup
import moe.styx.common.data.TokenMatchMethod
import moe.styx.common.data.TokenMatchType
import moe.styx.downloader.downloaderConfig
import moe.styx.downloader.utils.removeKeysFromURL
import moe.styx.web.topNotification

data class TokenGroupTemplatePreset(val name: String, val groups: List<TokenGroup>)

fun TextField.addRegexTemplateMenu(m: Media, rss: Boolean = false) {
    val field = this@addRegexTemplateMenu
    val jpName = resc((m.nameJP ?: m.name).replace(":", " "))
    val enName = resc((m.nameEN ?: m.name).replace(":", " "))
    val ext = if (rss) "" else "\\.mkv"
    contextMenu {
        target = this@addRegexTemplateMenu
        item("SubsPlease", { field.value = "\\[SubsPlease\\].*($jpName).*\\(1080p\\).*\\.mkv" })
        item("Erai-Raws", { field.value = "\\[Erai-Raws\\].*($jpName).*\\[1080p\\].*\\.mkv" })
        item("SubsPlus+", { field.value = "\\[SubsPlus\\+\\] ($enName) - S\\d+E\\d+.*WEB.*1080p.*ADN.*$ext" })
        item("GerFTP CR Sub", { field.value = "($jpName) E\\d+.* \\[1080p\\]\\[AAC\\]\\[JapDub\\]\\[GerEngSub\\].*\\.mkv" })
        item("GerFTP CR Dub", { field.value = "($jpName) E\\d+.* \\[1080p\\]\\[AAC\\]\\[GerJapDub\\]\\[GerEngSub\\].*\\.mkv" })
        item("GerFTP CR+AMZ Sub", { field.value = "($jpName) E\\d+.* \\[1080p\\]\\[E-?AC-?3\\].*\\[JapDub\\]\\[GerEngSub\\].*\\.mkv" })
        item("GerFTP CR+AMZ Dub", { field.value = "($jpName) E\\d+.* \\[1080p\\]\\[E-?AC-?3\\].*\\[GerJapDub\\]\\[GerEngSub\\].*\\.mkv" })
        item("Generic Fansub", { field.value = "\\[Group\\] ($jpName).*(?:1080p).*$ext" })
        item("Generic Scene", { field.value = "(${enName.replace(" ", ".")}).*1080p.*CR.*WEB-DL.*-Group$ext" })
    }
}

fun Component.addTokenGroupTemplateMenu(m: Media, onSelect: (List<TokenGroup>) -> Unit) {
    contextMenu {
        target = this@addTokenGroupTemplateMenu
        isOpenOnClick = true
        tokenGroupTemplatePresets(m).forEach { preset ->
            item(preset.name, { onSelect(preset.groups.deepCopy()) })
        }
    }
}

fun TextField.addRSSTemplateMenu() {
    val field = this@addRSSTemplateMenu
    contextMenu {
        target = this@addRSSTemplateMenu
        item("Nyaa RSS", { field.value = "https://nyaa.si/?page=rss&c=1_2&f=1&q=" })
        item("Tosho RSS", { field.value = "https://feed.animetosho.org/rss2?q=" })
        if (downloaderConfig.rssConfig.feedTemplates.isNotEmpty()) {
            downloaderConfig.rssConfig.feedTemplates.forEach { (name, url) ->
                item(name, {
                    field.value = "%$name%"
                    topNotification("Template goes to: ${removeKeysFromURL(url)}")
                })
            }
        }
    }
}

fun resc(s: String) = Regex.escapeReplacement(s)

private fun tokenGroupTemplatePresets(m: Media): List<TokenGroupTemplatePreset> {
    val jpName = (m.nameJP ?: m.name).replace(":", " ")
    val enName = (m.nameEN ?: m.name).replace(":", " ")
    val titleTokens = listOf(jpName, enName).filter { it.isNotBlank() }.distinctBy { it.lowercase() }
    val titleRegexTokens = titleTokens.map { regexTitleToken(it) }.distinctBy { it.lowercase() }

    val defaults = listOf(
        TokenGroupTemplatePreset(
            "SubsPlease",
            listOf(
                TokenGroup(tokens = listOf("SubsPlease", jpName, "1080p"), matchType = TokenMatchType.ALL)
            )
        ),
        TokenGroupTemplatePreset(
            "Erai-Raws",
            listOf(
                TokenGroup(tokens = listOf("Erai-Raws", jpName, "1080p"), matchType = TokenMatchType.ALL)
            )
        ),
        TokenGroupTemplatePreset(
            "GerFTP CR Sub",
            listOf(
                TokenGroup(tokens = listOf(jpName, "1080p", "AAC", "JapDub", "GerEngSub"), matchType = TokenMatchType.ALL)
            )
        ),
        TokenGroupTemplatePreset(
            "GerFTP CR Dub",
            listOf(
                TokenGroup(tokens = listOf(jpName, "1080p", "AAC", "GerJapDub", "GerEngSub"), matchType = TokenMatchType.ALL)
            )
        ),
        TokenGroupTemplatePreset(
            "Title",
            listOf(
                TokenGroup(tokens = titleTokens, matchType = TokenMatchType.ANY)
            )
        ),
        TokenGroupTemplatePreset(
            "Title (Regex)",
            listOf(
                TokenGroup(tokens = titleRegexTokens, method = TokenMatchMethod.REGEX, matchType = TokenMatchType.ANY)
            )
        ),
        TokenGroupTemplatePreset(
            "Dual-Audio",
            listOf(
                TokenGroup(tokens = listOf("Dual-Audio", "DUAL.(AAC|DDP)"), method = TokenMatchMethod.REGEX, matchType = TokenMatchType.ANY)
            )
        ),
        TokenGroupTemplatePreset(
            "Multi-Audio",
            listOf(
                TokenGroup(tokens = listOf("Multi-Audio", "MULTI.(AAC|DDP)"), method = TokenMatchMethod.REGEX, matchType = TokenMatchType.ANY)
            )
        ),
        TokenGroupTemplatePreset(
            "Single-Audio",
            listOf(
                TokenGroup(tokens = listOf("Multi-Audio", "MULTI.(AAC|DDP)", "Dual-Audio", "DUAL.(AAC|DDP)"), method = TokenMatchMethod.REGEX, matchType = TokenMatchType.NONE)
            )
        )
    )

    val configured = downloaderConfig.tokenGroupTemplates.map { (name, groups) ->
        TokenGroupTemplatePreset(name, groups.deepCopy())
    }
    return defaults + configured
}

private fun List<TokenGroup>.deepCopy(): List<TokenGroup> {
    return map { it.copy(tokens = it.tokens.toList()) }
}

private fun regexTitleToken(title: String): String {
    val normalized = title
        .trim()
        .replace(":", " ")
        .replace(Regex("(?i)\\b(\\d+)(st|nd|rd|th)\\s+Season\\b")) {
            "S${it.groupValues[1].toInt().toString().padStart(2, '0')}"
        }
        .replace(Regex("(?i)\\bSeason\\s+(\\d+)\\b")) {
            "S${it.groupValues[1].toInt().toString().padStart(2, '0')}"
        }
        .replace(Regex("\\s+"), " ")

    return resc(normalized).replace(" ", ".")
}
