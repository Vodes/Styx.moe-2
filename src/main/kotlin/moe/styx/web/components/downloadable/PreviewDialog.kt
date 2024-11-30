package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.data.value.ValueChangeMode
import moe.styx.common.data.DownloadableOption
import moe.styx.common.data.DownloaderTarget
import moe.styx.common.data.Media
import moe.styx.common.data.SourceType
import moe.styx.common.extension.readableSize
import moe.styx.downloader.downloaderConfig
import moe.styx.downloader.episodeWanted
import moe.styx.downloader.ftp.FTPClient
import moe.styx.downloader.ftp.FTPHandler
import moe.styx.downloader.parsing.ParseDenyReason
import moe.styx.downloader.parsing.ParseResult
import moe.styx.downloader.rss.FeedItem
import moe.styx.downloader.rss.RSSHandler
import moe.styx.web.components.addRSSTemplateMenu
import moe.styx.web.components.addRegexTemplateMenu
import moe.styx.web.createComponent
import moe.styx.web.replaceAll
import moe.styx.web.toReadableString
import org.vaadin.lineawesome.LineAwesomeIcon

class PreviewDialog(
    private val media: Media,
    private var target: DownloaderTarget,
    private var option: DownloadableOption,
    val onClose: (DownloadableOption) -> Unit
) : Dialog() {
    private lateinit var layout: VerticalLayout
    private var ftpClient: FTPClient? = null
    private val isRSS = option.source in arrayOf(SourceType.TORRENT, SourceType.USENET)
    private var pathChanged = true
    private var rssResults: List<RSSResult> = emptyList()
    private var ftpResults: List<FTPResult> = emptyList()

    init {
        setWidthFull()
        maxWidth = "1200px"

        run { downloaderConfig }

        if (!isRSS) {
            FTPHandler.initClient()
            ftpClient = if (!option.ftpConnectionString.isNullOrBlank())
                FTPClient.fromConnectionString(option.ftpConnectionString!!).connect()
            else
                FTPHandler.defaultFTPClient.connect()
        }

        verticalLayout {
            setWidthFull()
            h3("Downloader Preview")
            horizontalLayout {
                setWidthFull()
                maxWidth = "1000px"
                defaultVerticalComponentAlignment = FlexComponent.Alignment.END
                textField(if (isRSS) "RSS Feed" else "FTP Path") {
                    setWidthFull()
                    valueChangeMode = ValueChangeMode.LAZY
                    value = option.sourcePath ?: ""
                    if (isRSS)
                        addRSSTemplateMenu()
                    addValueChangeListener {
                        option = option.copy(sourcePath = it.value)
                        pathChanged = true
                        updateTarget()
                        updateResults()
                    }
                }
                if (isRSS)
                    iconButton(LineAwesomeIcon.EXTERNAL_LINK_SQUARE_ALT_SOLID.create()) {
                        onClick {
                            UI.getCurrent().page.open(option.sourcePath ?: "")
                        }
                    }
            }
            verticalLayout {
                setWidthFull()
                maxWidth = "1000px"
                textField("File Regex") {
                    setWidthFull()
                    valueChangeMode = ValueChangeMode.LAZY
                    value = option.fileRegex
                    addRegexTemplateMenu(media)
                    addValueChangeListener {
                        option = option.copy(fileRegex = it.value)
                        updateTarget()
                        updateResults()
                    }
                }
                if (isRSS)
                    textField("RSS Regex") {
                        setWidthFull()
                        valueChangeMode = ValueChangeMode.LAZY
                        value = option.rssRegex ?: ""
                        addRegexTemplateMenu(media, true)
                        addValueChangeListener {
                            option = option.copy(rssRegex = it.value)
                            updateTarget()
                            updateResults()
                        }
                    }
            }
            layout = verticalLayout {
                setSizeFull()
            }
        }.also {
            updateResults()
        }
    }

    private fun updateResults() {
        if (!isRSS && ftpClient == null) {
            layout.replaceAll { h4("Could not connect to the ftp server.") }
            return
        }

        val results = if (isRSS) {
            if (pathChanged || rssResults.isEmpty()) {
                pathChanged = false
                rssResults = RSSHandler.checkFeed(option.sourcePath!!, listOf(option), listOf(target))
            } else {
                rssResults = reevaluateRss()
            }
            rssResults.map { rssToDataClass(it, option.source == SourceType.USENET) }
        } else {
            if (pathChanged || ftpResults.isEmpty()) {
                pathChanged = false
                ftpResults = FTPHandler.checkDir(option.sourcePath!!, option, listOf(target), ftpClient!!)
            } else {
                ftpResults = reevaluateFtp()
            }
            ftpResults.map { ftpToDataClass(it) }
        }

        if (results.isEmpty()) {
            layout.replaceAll { h4("No entries could be found.") }
            return
        }

        layout.replaceAll {
            if (isRSS)
                init(rssResultsGrid(results))
            else
                init(ftpResultsGrid(results))
        }
    }

    override fun onDetach(detachEvent: DetachEvent?) {
        ftpClient?.disconnect()
        onClose(option)
    }

    private fun updateTarget() {
        // Ugly hack to remove references
        target = target.copy(options = target.options.toList().toMutableList())
        target.options.find { it.priority == option.priority }?.let {
            target.options[target.options.indexOf(it)] = option
        } ?: target.options.add(option)
    }

    private fun ftpResultsGrid(results: List<PreviewResult>) = createComponent {
        grid<PreviewResult> {
            setWidthFull()
            setItems(results)
            selectionMode = Grid.SelectionMode.NONE
            columnFor(PreviewResult::title, converter = { it!!.split("/").last() }, sortable = true) {
                setHeader("Filename")
                setTooltipGenerator(PreviewResult::title)
            }
            columnFor(PreviewResult::size, converter = { it!!.readableSize() }, sortable = true) {
                setHeader("Size")
                isExpand = false
            }
            columnFor(PreviewResult::parseResult, converter = { it!!.toReadableString() }, sortable = false) {
                setHeader("Result")
                setWidth("250px")
                isExpand = false
            }
            gridContextMenu {
                item("Check parsing", clickListener = {
                    ParsingDialog(it!!.title.split("/").last(), false, target = this@PreviewDialog.target).open()
                })
            }
        }
    }

    private fun rssResultsGrid(results: List<PreviewResult>) = createComponent {
        grid<PreviewResult> {
            setWidthFull()
            setItems(results)
            selectionMode = Grid.SelectionMode.NONE
            columnFor(PreviewResult::title, converter = { s -> s!!.split("/").maxBy { it.length } }, sortable = true) {
                setHeader("Title")
                setTooltipGenerator(PreviewResult::title)
            }
            columnFor(PreviewResult::parseResult, converter = { it!!.toReadableString() }, sortable = false) {
                setHeader("Result")
                setWidth("250px")
                isExpand = false
            }
            gridContextMenu {
                item("Open Post", clickListener = {
                    UI.getCurrent().page.open(it!!.postURL)
                })
                item("Download Torrent", clickListener = {
                    UI.getCurrent().page.open(it!!.torrentURL)
                })
                item("Check parsing", clickListener = {
                    ParsingDialog(it!!.title, true, target = this@PreviewDialog.target).open()
                })
            }
        }
    }

    private fun reevaluateRss(): List<RSSResult> {
        val new = rssResults.map {
            var parseResult = it.second
            if (it.second !is ParseResult.DENIED || (it.second as ParseResult.DENIED).parseFailReason != ParseDenyReason.PostIsTooOld) {
                parseResult = option.episodeWanted(it.first.title, null, target, true)
            }
            return@map it.first to parseResult
        }
        return new
    }

    private fun reevaluateFtp(): List<FTPResult> {
        val new = ftpResults.map {
            var parseResult = it.second
            if (it.second !is ParseResult.DENIED || (it.second as ParseResult.DENIED).parseFailReason != ParseDenyReason.PostIsTooOld) {
                parseResult = option.episodeWanted(it.first.first, (it.second as? ParseResult.OK)?.parentDir, target)
            }
            return@map it.first to parseResult
        }
        return new
    }
}

private typealias FTPResult = Pair<Pair<String, Long>, ParseResult>
private typealias RSSResult = Pair<FeedItem, ParseResult>

data class PreviewResult(val title: String, val postURL: String = "", val torrentURL: String = "", val size: Long = 0L, val parseResult: ParseResult)

fun ftpToDataClass(result: FTPResult): PreviewResult {
    return PreviewResult(result.first.first, "", "", result.first.second, result.second)
}

fun rssToDataClass(result: RSSResult, usenet: Boolean = false): PreviewResult {
    return PreviewResult(
        result.first.title,
        result.first.getPostURL(),
        if (!usenet) result.first.getTorrentURL() else result.first.getNZBURL() ?: "",
        0L,
        result.second
    )
}