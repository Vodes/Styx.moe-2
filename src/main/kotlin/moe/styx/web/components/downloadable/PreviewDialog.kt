package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.selectionMode
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.AnchorTarget
import kotlinx.datetime.Clock
import moe.styx.downloader.ftp.FTPClient
import moe.styx.downloader.ftp.FTPHandler
import moe.styx.downloader.loadConfig
import moe.styx.downloader.parsing.ParseResult
import moe.styx.downloader.torrent.FeedItem
import moe.styx.downloader.torrent.RSSHandler
import moe.styx.types.DownloadableOption
import moe.styx.types.DownloaderTarget
import moe.styx.types.Media
import moe.styx.types.SourceType
import moe.styx.web.readableSize
import moe.styx.downloader.Main as DownloaderMain

var lastInitialized = 0L

class PreviewDialog(private val media: Media, private val target: DownloaderTarget, private val option: DownloadableOption) : Dialog() {
    init {
        println(option)
        setWidthFull()
        maxWidth = "1200px"
        if (!DownloaderMain.isInitialized() || lastInitialized < (Clock.System.now().epochSeconds - 300)) {
            loadConfig()
            lastInitialized = Clock.System.now().epochSeconds
        }
        verticalLayout {
            setWidthFull()
            h3("Downloader Preview")
            horizontalLayout {
                nativeLabel("Feed/Path: ")
                if (option.source == SourceType.FTP)
                    nativeLabel(option.sourcePath)
                else
                    anchor(option.sourcePath!!, option.sourcePath!!) {
                        setTarget(AnchorTarget.BLANK)
                    }
            }
            if (option.source == SourceType.FTP) {
                FTPHandler.initClient()
                val connected = if (!option.ftpConnectionString.isNullOrBlank()) {
                    FTPClient.fromConnectionString(option.ftpConnectionString!!).connect()
                } else
                    FTPHandler.defaultFTPClient.connect()
                if (connected == null) {
                    h4("Could not authenticate the FTP Client.")
                    return@verticalLayout
                }
                val results = FTPHandler.checkDir(option.sourcePath!!, option, listOf(target), connected)
                verticalLayout Layout@{
                    if (results.isEmpty()) {
                        h4("No files found.")
                        return@Layout
                    }
                    grid<PreviewResult> {
                        setWidthFull()
                        setItems(results.map { ftpToDataClass(it) })
                        selectionMode = Grid.SelectionMode.NONE
                        columnFor(PreviewResult::title, converter = { it!!.split("/").last() }, sortable = true) {
                            setHeader("Filename")
                            setTooltipGenerator(PreviewResult::title)
                        }
                        columnFor(PreviewResult::size, converter = { it!!.readableSize() }, sortable = true) {
                            setHeader("Size")
                            isExpand = false
                        }
                        columnFor(PreviewResult::parseResult, converter = {
                            return@columnFor when (it!!) {
                                is ParseResult.OK -> "Would download"
                                is ParseResult.DENIED -> "Denied: ${(it as ParseResult.DENIED).parseFailReason.name}"
                                else -> "Failed: ${(it as ParseResult.FAILED).parseFailReason.name}"
                            }
                        }, sortable = false) {
                            setHeader("Result")
                            setWidth("250px")
                            isExpand = false
                        }
                    }
                }
            } else {
                val results = RSSHandler.checkFeed(option.sourcePath!!, listOf(option), listOf(target))
                verticalLayout Layout@{
                    if (results.isEmpty()) {
                        h4("No entries found.")
                        return@Layout
                    }
                    grid<PreviewResult> {
                        setWidthFull()
                        setItems(results.map { rssToDataClass(it) })
                        selectionMode = Grid.SelectionMode.NONE
                        columnFor(PreviewResult::title, converter = { it!!.split("/").last() }, sortable = true) {
                            setHeader("Title")
                            setTooltipGenerator(PreviewResult::title)
                        }
                        columnFor(PreviewResult::parseResult, converter = {
                            return@columnFor when (it!!) {
                                is ParseResult.OK -> "Would download"
                                is ParseResult.DENIED -> "Denied: ${(it as ParseResult.DENIED).parseFailReason.name}"
                                else -> "Failed: ${(it as ParseResult.FAILED).parseFailReason.name}"
                            }
                        }, sortable = false) {
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
                        }
                    }
                }
            }
        }
    }
}

private typealias FTPResult = Pair<Pair<String, Long>, ParseResult>
private typealias RSSResult = Pair<FeedItem, ParseResult>

data class PreviewResult(val title: String, val postURL: String = "", val torrentURL: String = "", val size: Long = 0L, val parseResult: ParseResult)

fun ftpToDataClass(result: FTPResult): PreviewResult {
    return PreviewResult(result.first.first, "", "", result.first.second, result.second)
}

fun rssToDataClass(result: RSSResult): PreviewResult {
    return PreviewResult(result.first.title, result.first.getPostURL(), result.first.getTorrentURL(), 0L, result.second)
}
