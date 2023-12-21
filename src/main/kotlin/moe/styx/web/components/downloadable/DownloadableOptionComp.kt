package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.types.DownloadableOption
import moe.styx.types.SourceType
import moe.styx.web.capitalize

class DLOptionComponent(private var option: DownloadableOption, onUpdate: (DownloadableOption) -> DownloadableOption) : KComposite() {
    private lateinit var rssRegexField: TextField
    private lateinit var pathField: TextField
    private lateinit var ftpConnField: TextField
    private lateinit var keepSeedingCheck: Checkbox

    val root = ui {
        verticalLayout {
            setWidthFull()
            isPadding = false
            isSpacing = false
            textField("File Regex") {
                value = option.fileRegex
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener { option = onUpdate(option.copy(fileRegex = it.value)) }
                setWidthFull()
                maxWidth = "500px"
            }
            flexLayout {
                addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL, "flex-container")
                setWidthFull()
                maxWidth = "1550px"
                val typeSelect = select<SourceType>("Source") {
                    setItems(SourceType.entries)
                    value = option.source
                    isEmptySelectionAllowed = false
                    setTextRenderer { if (it.name.length > 4) it.name.capitalize() else it.name }
                    addValueChangeListener {
                        option = onUpdate(option.copy(source = it.value))
                        updateVisibility(it.value)
                    }
                    flexGrow = 1.0
                    maxWidth = "200px"
                }
                rssRegexField = textField("RSS Regex") {
                    setTooltipText("File Regex will be used if empty")
                    isVisible = option.source == SourceType.TORRENT
                    value = option.rssRegex ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { option = onUpdate(option.copy(rssRegex = it.value)) }
                    flexGrow = 1.0
                }
                pathField = textField(if (option.source == SourceType.TORRENT) "RSS Feed" else "FTP Path") {
                    isVisible = option.source in arrayOf(SourceType.TORRENT, SourceType.FTP)
                    value = option.sourcePath ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { option = onUpdate(option.copy(sourcePath = it.value)) }
                    flexGrow = 1.0
                }
                ftpConnField = textField("FTP ConnectionString") {
                    isVisible = option.source == SourceType.FTP
                    value = option.ftpConnectionString ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { option = onUpdate(option.copy(ftpConnectionString = it.value)) }
                    flexGrow = 1.0
                }
                keepSeedingCheck = checkBox("Keep Seeding") {
                    addClassNames("meme-checkbox")
                    isVisible = option.source == SourceType.TORRENT
                    value = option.keepSeeding
                    addValueChangeListener { option = onUpdate(option.copy(keepSeeding = it.value)) }
                    height = "35px"
                }
                setAlignSelf(FlexComponent.Alignment.START, typeSelect)
            }
        }
    }

    private fun updateVisibility(source: SourceType) {
        when (source) {
            SourceType.TORRENT -> {
                rssRegexField.isVisible = true
                pathField.isVisible = true
                keepSeedingCheck.isVisible = true
                ftpConnField.isVisible = false
                pathField.label = "RSS Feed"
            }

            SourceType.FTP -> {
                rssRegexField.isVisible = false
                keepSeedingCheck.isVisible = false
                ftpConnField.isVisible = true
                pathField.isVisible = true
                pathField.label = "FTP Path"
            }

            else -> {
                keepSeedingCheck.isVisible = false
                rssRegexField.isVisible = false
                ftpConnField.isVisible = false
                pathField.isVisible = false
            }
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).dlOpComponent(
    option: DownloadableOption, onUpdate: (DownloadableOption) -> DownloadableOption,
    block: (@VaadinDsl DLOptionComponent).() -> Unit = {}
) = init(
    DLOptionComponent(option, onUpdate), block
)