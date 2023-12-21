package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.karibudsl.v23.tab
import com.github.mvysny.karibudsl.v23.tabSheet
import com.github.mvysny.kaributools.setClassNames2
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.tabs.TabSheet
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.types.DownloadableOption
import moe.styx.types.DownloaderTarget
import moe.styx.types.Media
import moe.styx.web.createComponent
import moe.styx.web.replaceAll

class DownloadableOverview(var target: DownloaderTarget, val media: Media) : KComposite() {
    private lateinit var layout: VerticalLayout
    private var tabSheet: TabSheet? = null
    private lateinit var removeButton: Button

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
            }
            layout = verticalLayout {
                isPadding = false
                isSpacing = false

                add(initTargetView())
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
        }
    }

    private fun initTargetView() = createComponent {
        if (target.options.isEmpty())
            h3("No Options added yet!")
        else
            tabSheet {
                setSizeFull()
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