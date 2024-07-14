package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.NativeLabel
import com.vaadin.flow.data.value.ValueChangeMode
import moe.styx.common.data.DownloaderTarget
import moe.styx.db.tables.DownloaderTargetsTable
import moe.styx.downloader.episodeWanted
import moe.styx.downloader.parsing.parseMetadata
import moe.styx.web.dbClient
import moe.styx.web.toReadableString
import org.jetbrains.exposed.sql.selectAll

class ParsingDialog(toParse: String, val rss: Boolean, openedForLocal: Boolean = false, val target: DownloaderTarget? = null) : Dialog() {

    lateinit var parsingGrid: Grid<MappedParsing>
    lateinit var resultLabel: NativeLabel

    init {
        isModal = true
        isDraggable = true

        verticalLayout {
            width = "95%"
            maxWidth = "500px"
            h2("Metadata Parsing")
            textField("To parse") {
                setWidthFull()
                value = toParse
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener {
                    if (!it.value.isNullOrBlank())
                        updateResults(it.value)
                    else {
                        parsingGrid.setItems(emptyList())
                        resultLabel.text = ""
                    }
                }
            }
            if (openedForLocal)
                h5("XDCC/Local do not support previewing. Feel free to test the parsing here.")
            parsingGrid = grid<MappedParsing> {
                setWidthFull()
                columnFor(MappedParsing::category) { setHeader("Type"); setFlexGrow(1) }
                columnFor(MappedParsing::value) { setHeader("Value"); setFlexGrow(1) }
            }
            h3("EP wanted?")
            resultLabel = nativeLabel("")
        }.also {
            updateResults(toParse)
        }
    }

    private fun updateResults(input: String) {
        val parsedMeta = parseMetadata(input)
        val meta = parsedMeta.map { MappedParsing(it.category.name.replace("kElement", ""), it.value) }

        val targets = target?.let { listOf(target) } ?: dbClient.transaction { DownloaderTargetsTable.query { selectAll().toList() } }
        val resultString = targets.episodeWanted(input, rss).toReadableString()

        parsingGrid.setItems(meta)
        resultLabel.text = resultString
    }
}

data class MappedParsing(val category: String, val value: String)