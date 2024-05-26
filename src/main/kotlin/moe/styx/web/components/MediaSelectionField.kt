package moe.styx.web.components

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility.Gap
import moe.styx.db.tables.MediaTable
import moe.styx.web.components.media.MediaChooseDialog
import moe.styx.web.dbClient
import org.jetbrains.exposed.sql.selectAll
import org.vaadin.lineawesome.LineAwesomeIcon

class MediaSelectionField(title: String, private var selected: String, onSelect: (String) -> Unit, exclude: String? = null) : KComposite() {

    val root = ui {
        horizontalLayout {
            isPadding = false
            isSpacing = false
            addClassNames(Gap.SMALL)
            defaultVerticalComponentAlignment = FlexComponent.Alignment.END
            val selectionField = textField(title) {
                value = selected
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener { onSelect(it.value.trim()) }
                setWidthFull()
                val tooltip = tooltip.withManual(true)
                val tooltipButton = Button(LineAwesomeIcon.INFO_SOLID.create())
                suffixComponent = tooltipButton
                tooltipButton.onClick {
                    if (value.isNullOrBlank()) {
                        if (tooltip.isOpened)
                            tooltip.isOpened = false
                        return@onClick
                    }
                    if (!tooltip.isOpened) {
                        tooltip.text = dbClient.transaction {
                            MediaTable.query { selectAll().where { GUID eq value }.toList() }.firstOrNull()?.name ?: "No media found."
                        }
                    }
                    tooltip.isOpened = !tooltip.isOpened;
                }
            }
            iconButton(LineAwesomeIcon.SEARCH_SOLID.create()) {
                onClick { MediaChooseDialog(exclude ?: "") { selectionField.value = it?.GUID ?: "" }.open() }
                height = "39px"
            }
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).mediaSelection(
    title: String, selected: String, onSelect: (String) -> Unit, exclude: String? = null,
    block: (@VaadinDsl MediaSelectionField).() -> Unit = {}
) = init(
    MediaSelectionField(title, selected, onSelect, exclude), block
)