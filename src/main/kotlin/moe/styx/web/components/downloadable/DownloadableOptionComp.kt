package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility
import com.vaadin.flow.theme.lumo.LumoUtility.Padding
import moe.styx.common.data.DownloadableOption
import moe.styx.common.data.Media
import moe.styx.common.data.ProcessingOptions
import moe.styx.common.data.SourceType
import moe.styx.common.extension.capitalize
import moe.styx.web.components.EditorState
import moe.styx.web.components.addRSSTemplateMenu
import moe.styx.web.components.addRegexTemplateMenu
import moe.styx.web.createComponent

class DLOptionComponent(
    private val media: Media,
    private val optionState: EditorState<DownloadableOption>
) :
    KComposite() {
    private lateinit var matchingLayout: VerticalLayout
    private lateinit var rssRegexField: TextField
    private lateinit var pathField: TextField
    private lateinit var ftpConnField: TextField
    private lateinit var keepSeedingCheck: Checkbox
    private lateinit var ignoreParentCheck: Checkbox
    private lateinit var processingButton: Button

    private fun currentOption() = optionState.current()

    private fun updateOption(update: (DownloadableOption) -> DownloadableOption) = optionState.update(update)

    val root = ui {
        val option = currentOption()
        verticalLayout {
            setWidthFull()
            isPadding = false
            isSpacing = false
            h3("Matching") { addClassNames(Padding.Bottom.MEDIUM) }
            checkBox("Use token groups") {
                addClassNames("left-aligned-checkbox")
                addClassNames("meme-checkbox")
                value = option.useTokens
                addValueChangeListener {
                    updateOption { option -> option.copy(useTokens = it.value) }
                    renderMatchingFields()
                    updateVisibility(currentOption().source)
                }
                height = "35px"
            }
            matchingLayout = verticalLayout {
                setWidthFull()
                isPadding = false
                isSpacing = false
            }
            h3("Source") { addClassNames(Padding.Vertical.MEDIUM) }
            flexLayout {
                addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL, "flex-container")
                setWidthFull()
                maxWidth = "1550px"
                val typeSelect = select<SourceType>("Type") {
                    setItems(SourceType.entries)
                    value = option.source
                    isEmptySelectionAllowed = false
                    setTextRenderer { if (it.name.length > 4) it.name.capitalize() else it.name }
                    addValueChangeListener {
                        updateOption { option -> option.copy(source = it.value) }
                        updateVisibility(it.value)
                    }
                    flexGrow = 1.0
                    maxWidth = "200px"
                }
                rssRegexField = textField("RSS Regex") {
                    setTooltipText("File Regex will be used if empty")
                    isVisible = !option.useTokens && option.source in arrayOf(SourceType.TORRENT, SourceType.USENET)
                    value = option.rssRegex ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addRegexTemplateMenu(media, true)
                    addValueChangeListener { updateOption { option -> option.copy(rssRegex = it.value) } }
                    flexGrow = 1.0
                }
                pathField = textField(if (option.source in arrayOf(SourceType.TORRENT, SourceType.USENET)) "RSS Feed" else "FTP Path") {
                    isVisible = option.source in arrayOf(SourceType.TORRENT, SourceType.USENET, SourceType.FTP)
                    value = option.sourcePath ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addRSSTemplateMenu()
                    addValueChangeListener { updateOption { option -> option.copy(sourcePath = it.value) } }
                    flexGrow = 1.0
                }
                ftpConnField = textField("FTP ConnectionString") {
                    isVisible = option.source == SourceType.FTP
                    value = option.ftpConnectionString ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { updateOption { option -> option.copy(ftpConnectionString = it.value) } }
                    flexGrow = 1.0
                }
                keepSeedingCheck = checkBox("Keep Seeding") {
                    addClassNames("meme-checkbox")
                    isVisible = option.source == SourceType.TORRENT
                    value = option.keepSeeding
                    addValueChangeListener { updateOption { option -> option.copy(keepSeeding = it.value) } }
                    height = "35px"
                }
                setAlignSelf(FlexComponent.Alignment.START, typeSelect)
            }
            h3("Parsing") { addClassNames(Padding.Vertical.MEDIUM) }
            flexLayout {
                addClassNames(LumoUtility.AlignItems.START, LumoUtility.Gap.MEDIUM, "flex-container")
                setWidthFull()
                maxWidth = "1550px"
                integerField("Episode Offset") {
                    value = option.episodeOffset ?: 0
                    isStepButtonsVisible = true
                    step = 1
                    width = "200px"
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { updateOption { option -> option.copy(episodeOffset = it.value) } }
                }
                horizontalLayout(false) {
                    addClassNames(LumoUtility.Gap.MEDIUM, "checkbox-horilayout")
                    checkBox("Ignore Delay") {
                        addClassNames("meme-checkbox")
                        setTooltipText("Also Download stuff that's older than an hour.")
                        value = option.ignoreDelay
                        addValueChangeListener { updateOption { option -> option.copy(ignoreDelay = it.value) } }
                        height = "35px"
                    }
                    checkBox("Wait for previous") {
                        addClassNames("meme-checkbox")
                        setTooltipText("Require previous option to exist before downloading")
                        value = option.waitForPrevious
                        addValueChangeListener { updateOption { option -> option.copy(waitForPrevious = it.value) } }
                        height = "35px"
                    }
                    ignoreParentCheck = checkBox("Ignore Parent Folder") {
                        addClassNames("meme-checkbox")
                        setTooltipText("Ignore parent folder when checking for FTP matches.")
                        value = option.ignoreParentFolder
                        isVisible = option.source == SourceType.FTP
                        addValueChangeListener { updateOption { option -> option.copy(ignoreParentFolder = it.value) } }
                        height = "35px"
                    }
                }
            }
            h3("Overrides") { addClassNames(Padding.Vertical.MEDIUM) }
            flexLayout {
                addClassNames(LumoUtility.AlignItems.START, LumoUtility.Gap.MEDIUM, "flex-container")
                setWidthFull()
                maxWidth = "1550px"
                textField("Naming Template") {
                    value = option.overrideNamingTemplate ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { updateOption { option -> option.copy(overrideNamingTemplate = it.value) } }
                    flexGrow = 1.0
                }
                textField("Title Template") {
                    value = option.overrideTitleTemplate ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { updateOption { option -> option.copy(overrideTitleTemplate = it.value) } }
                    flexGrow = 1.0
                }
                textField("Command") {
                    value = option.commandAfter ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { updateOption { option -> option.copy(commandAfter = it.value) } }
                    flexGrow = 1.0
                }
            }
            horizontalLayout(true) {
                defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                checkBox("Processing") {
                    value = option.processingOptions != null
                    addValueChangeListener {
                        updateOption { option -> option.copy(processingOptions = if (!it.value) null else ProcessingOptions()) }
                        processingButton.isEnabled = it.value
                    }
                }
                processingButton = button("Choose processing") {
                    isEnabled = option.processingOptions != null
                    onClick {
                        val option = currentOption()
                        if (option.processingOptions == null)
                            return@onClick
                        ProcessingDialog(option.processingOptions!!, option.priority) {
                            updateOption { option -> option.copy(processingOptions = it) }
                        }.open()
                    }
                }
            }
        }.also {
            renderMatchingFields()
        }
    }

    private fun renderMatchingFields() {
        matchingLayout.removeAll()
        val option = currentOption()
        if (option.useTokens) {
            matchingLayout.add(createComponent {
                tokenGroupsComponent(option.tokenGroups, {
                    updateOption { option -> option.copy(tokenGroups = it) }
                })
            })
            return
        }

        matchingLayout.add(createComponent {
            textField("File Regex") {
                value = option.fileRegex
                valueChangeMode = ValueChangeMode.LAZY
                addRegexTemplateMenu(media)
                addValueChangeListener { updateOption { option -> option.copy(fileRegex = it.value) } }
                setWidthFull()
                maxWidth = "600px"
            }
        })
    }

    private fun updateVisibility(source: SourceType) {
        val option = currentOption()
        when (source) {
            SourceType.TORRENT, SourceType.USENET -> {
                rssRegexField.isVisible = !option.useTokens
                pathField.isVisible = true
                keepSeedingCheck.isVisible = source == SourceType.TORRENT
                ftpConnField.isVisible = false
                ignoreParentCheck.isVisible = false
                pathField.label = "RSS Feed"
            }

            SourceType.FTP -> {
                rssRegexField.isVisible = false
                keepSeedingCheck.isVisible = false
                ftpConnField.isVisible = true
                ignoreParentCheck.isVisible = true
                pathField.isVisible = true
                pathField.label = "FTP Path"
            }

            else -> {
                rssRegexField.isVisible = false
                keepSeedingCheck.isVisible = false
                ignoreParentCheck.isVisible = false
                ftpConnField.isVisible = false
                pathField.isVisible = false
            }
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).dlOpComponent(
    media: Media, optionState: EditorState<DownloadableOption>,
    block: (@VaadinDsl DLOptionComponent).() -> Unit = {}
) = init(
    DLOptionComponent(media, optionState), block
)
