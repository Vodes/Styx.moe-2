package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.data.value.ValueChangeMode
import moe.styx.types.DownloadableOption

class DLOptionComponent(private var option: DownloadableOption, onUpdate: (DownloadableOption) -> DownloadableOption) : KComposite() {
    val root = ui {
        verticalLayout {
            isPadding = false
            isSpacing = false
            textField("File Regex") {
                value = option.fileRegex
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener { option = onUpdate(option.copy(fileRegex = it.value)) }
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