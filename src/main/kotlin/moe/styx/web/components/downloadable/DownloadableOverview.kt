package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.karibudsl.v23.tab
import com.github.mvysny.karibudsl.v23.tabSheet
import com.github.mvysny.kaributools.setClassNames2
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.types.DownloaderTarget
import moe.styx.types.Media
import moe.styx.web.createComponent
import moe.styx.web.replaceAll

class DownloadableOverview(var target: DownloaderTarget, val media: Media) : KComposite() {
    private lateinit var layout: VerticalLayout

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
            layout = verticalLayout {
                isPadding = false
                isSpacing = false

                add(initTargetView())
            }
        }
    }

    private fun updateTargetRef(func: () -> Unit) = func().run { layout.replaceAll { initTargetView() } }

    private fun initTargetView() = createComponent {
        if (target.options.isEmpty())
            h3("No Options added yet!")
        else
            tabSheet {
                setSizeFull()
                target.options.forEach {
                    tab(it.fileRegex) {
                        h2("Lol")
                    }
                }
            }
    }

}