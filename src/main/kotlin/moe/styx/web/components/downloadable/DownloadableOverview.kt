package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.karibudsl.v23.tab
import com.github.mvysny.karibudsl.v23.tabSheet
import com.github.mvysny.kaributools.setClassNames2
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.tabs.TabSheet
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.db.delete
import moe.styx.db.save
import moe.styx.types.DownloadableOption
import moe.styx.types.DownloaderTarget
import moe.styx.types.Media
import moe.styx.types.SourceType
import moe.styx.web.createComponent
import moe.styx.web.getDBClient
import moe.styx.web.replaceAll
import moe.styx.web.topNotification

class DownloadableOverview(var target: DownloaderTarget, val media: Media) : KComposite() {
    private lateinit var layout: VerticalLayout
    private var tabSheet: TabSheet? = null
    private lateinit var removeButton: Button
    private lateinit var moveUpButton: Button
    private lateinit var moveDownButton: Button

    val root = ui {
        verticalLayout {
            h3("Editing Downloader profile for ${media.name}")
            flexLayout {
                setClassNames2(LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL, "flex-container")
                setWidthFull()
                maxWidth = "1550px"
                textField("Naming Template") {
                    value = target.namingTemplate
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { updateTargetRef { target = target.copy(namingTemplate = value) } }
                    flexGrow = 1.0
                }
                textField("Title Template") {
                    value = target.titleTemplate
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { updateTargetRef { target = target.copy(titleTemplate = value) } }
                    flexGrow = 1.0
                }
                textField("Output Dir") {
                    value = target.outputDir
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { updateTargetRef { target = target.copy(outputDir = value) } }
                    flexGrow = 1.0
                }
            }
            horizontalLayout {
                button("Add") {
                    onLeftClick {
                        val num = target.options.maxOfOrNull { it.priority } ?: -1
                        target.options.add(DownloadableOption(num + 1, ""))
                        updateTargetRef()
                    }
                }
                removeButton = button("Remove") {
                    addThemeVariants(ButtonVariant.LUMO_ERROR)
                    isEnabled = target.options.isNotEmpty()
                    onLeftClick {
                        val num = tabSheet?.selectedTab?.label?.toIntOrNull()
                        if (target.options.removeIf { it.priority == num }) {
                            tabSheet?.remove(tabSheet!!.selectedTab)
                            reorderOptions()
                            updateTargetRef()
                        }
                    }
                }
                moveDownButton = button("Move down") {
                    isEnabled = (tabSheet?.selectedTab?.label?.toIntOrNull() ?: 0) > 0
                    onLeftClick {
                        val num = tabSheet?.selectedTab?.label?.toIntOrNull()
                        if (num != null) {
                            target.options[num] = target.options[num].copy(priority = num - 1)
                            target.options[num - 1] = target.options[num - 1].copy(priority = num)
                            reorderOptions()
                            updateTargetRef()
                            tabSheet?.let { it.selectedIndex = num - 1 }
                        }
                    }
                }
                moveUpButton = button("Move up") {
                    isEnabled = (tabSheet?.selectedTab?.label?.toIntOrNull() ?: 0) < (target.options.size - 1)
                    onLeftClick {
                        val num = tabSheet?.selectedTab?.label?.toIntOrNull()
                        if (num != null) {
                            target.options[num] = target.options[num].copy(priority = num + 1)
                            target.options[num + 1] = target.options[num + 1].copy(priority = num)
                            reorderOptions()
                            updateTargetRef()
                            tabSheet?.let { it.selectedIndex = num + 1 }
                        }
                    }
                }
                button("Preview") {
                    onLeftClick {
                        if (target.options.isEmpty())
                            topNotification("No targets in the list.").also { return@onLeftClick }

                        val num = tabSheet?.selectedTab?.label?.toIntOrNull() ?: return@onLeftClick
                        val option = target.options[num]
                        if (option.source in listOf(SourceType.LOCAL, SourceType.XDCC) || (option.source in listOf(
                                SourceType.TORRENT,
                                SourceType.FTP
                            ) && option.sourcePath.isNullOrBlank())
                        ) {
                            if (option.source in listOf(SourceType.LOCAL, SourceType.XDCC))
                                topNotification("Cannot preview XDCC or Local.")
                            else
                                topNotification("Please make sure you have a valid rss feed or ftp path.")
                            return@onLeftClick
                        }
                        PreviewDialog(media, target, option).open()
                    }
                }
            }
            layout = verticalLayout {
                isPadding = false
                isSpacing = false

                add(initTargetView())
            }
            horizontalLayout {
                button("Save") {
                    addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SUCCESS)
                    onLeftClick {
                        getDBClient().executeAndClose {
                            if (target.options.isEmpty())
                                delete(target)
                            else
                                save(target)
                        }
                        UI.getCurrent().page.history.back()
                    }
                }
            }
        }
    }

    private fun reorderOptions() {
        if (target.options.isEmpty())
            return

        target.options.sortedBy { it.priority }.forEachIndexed { index, option ->
            target.options[index] = option.copy(priority = index)
        }
    }

    private fun updateTargetRef(func: () -> Unit = {}) {
        func().run {
            layout.replaceAll { initTargetView() }
            removeButton.isEnabled = target.options.isNotEmpty()
            moveUpButton.isEnabled = (tabSheet?.selectedTab?.label?.toIntOrNull() ?: 0) < (target.options.size - 1)
            moveDownButton.isEnabled = (tabSheet?.selectedTab?.label?.toIntOrNull() ?: 0) > 0
        }
    }

    private fun initTargetView() = createComponent {
        if (target.options.isEmpty())
            h3("No Options added yet!")
        else
            tabSheet {
                setSizeFull()
                addSelectedChangeListener {
                    moveUpButton.isEnabled = (tabSheet?.selectedTab?.label?.toIntOrNull() ?: 0) < (target.options.size - 1)
                    moveDownButton.isEnabled = (tabSheet?.selectedTab?.label?.toIntOrNull() ?: 0) > 0
                }
                target.options.forEachIndexed { index, option ->
                    tab(option.priority.toString()) {
                        dlOpComponent(option, {
                            target.options[index] = it
                            it
                        })
                    }
                }
            }.also { tabSheet = it }
    }

}