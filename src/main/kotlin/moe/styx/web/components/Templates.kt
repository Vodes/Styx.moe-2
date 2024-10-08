package moe.styx.web.components

import com.github.mvysny.karibudsl.v10.contextMenu
import com.github.mvysny.karibudsl.v10.item
import com.vaadin.flow.component.textfield.TextField
import moe.styx.common.data.Media
import moe.styx.downloader.downloaderConfig
import moe.styx.downloader.utils.removeKeysFromURL
import moe.styx.web.topNotification

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