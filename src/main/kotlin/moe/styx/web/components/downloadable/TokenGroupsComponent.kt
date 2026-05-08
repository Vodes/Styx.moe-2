package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.common.data.TokenGroup
import moe.styx.common.data.TokenMatchMethod
import moe.styx.common.data.TokenMatchType
import moe.styx.common.data.TokenTarget
import moe.styx.web.createComponent
import org.vaadin.lineawesome.LineAwesomeIcon

class TokenGroupsComponent(
    private var tokenGroups: List<TokenGroup>,
    private val onUpdate: (List<TokenGroup>) -> Unit
) : KComposite() {
    private lateinit var groupLayout: VerticalLayout

    val root = ui {
        verticalLayout {
            setWidthFull()
            isPadding = false
            isSpacing = false
            groupLayout = verticalLayout {
                setWidthFull()
                isPadding = false
            }
            horizontalLayout {
                defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                iconButton(LineAwesomeIcon.PLUS_SOLID.create()) {
                    setTooltipText("Add token group")
                    onClick {
                        updateGroups(tokenGroups + TokenGroup(), true)
                    }
                }
            }
        }.also {
            renderGroups()
        }
    }

    private fun renderGroups() {
        groupLayout.removeAll()
        if (tokenGroups.isEmpty()) {
            groupLayout.add(createComponent { h4("No token groups added yet!") })
            return
        }

        tokenGroups.forEachIndexed { index, group ->
            groupLayout.add(createComponent {
                verticalLayout {
                    setWidthFull()
                    isPadding = false
                    lateinit var tokensField: TextArea
                    if (index != 0)
                        addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_30, LumoUtility.Padding.Top.MEDIUM)

                    horizontalLayout {
                        defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                        addClassNames(LumoUtility.Gap.SMALL, "flex-container", "token-group-row")
                        setWidthFull()

                        select<TokenTarget>("Target") {
                            setItems(TokenTarget.entries)
                            value = group.target
                            isEmptySelectionAllowed = false
                            setTextRenderer { it.label() }
                            addValueChangeListener {
                                val updated = group.copy(target = it.value)
                                validateTokens(updated, tokensField)
                                updateGroup(index, updated)
                            }
                            width = "130px"
                        }
                        select<TokenMatchMethod>("Method") {
                            setItems(TokenMatchMethod.entries)
                            value = group.method
                            isEmptySelectionAllowed = false
                            setTextRenderer { it.label() }
                            addValueChangeListener {
                                val updated = group.copy(method = it.value)
                                validateTokens(updated, tokensField)
                                updateGroup(index, updated)
                            }
                            width = "150px"
                        }
                        select<TokenMatchType>("Match") {
                            setItems(TokenMatchType.entries)
                            value = group.matchType
                            isEmptySelectionAllowed = false
                            setTextRenderer { it.label() }
                            addValueChangeListener {
                                val updated = group.copy(matchType = it.value)
                                validateTokens(updated, tokensField)
                                updateGroup(index, updated)
                            }
                            width = "130px"
                        }
                        iconButton(LineAwesomeIcon.TRASH_SOLID.create()) {
                            addClassNames("token-group-delete")
                            setTooltipText("Remove token group")
                            addThemeVariants(ButtonVariant.LUMO_ERROR)
                            onClick {
                                updateGroups(tokenGroups.filterIndexed { i, _ -> i != index }, true)
                            }
                        }
                    }

                    tokensField = textArea("Tokens") {
                        setWidthFull()
                        maxWidth = "700px"
                        value = group.tokens.joinToString("\n")
                        placeholder = "Erai\nVARYG"
                        valueChangeMode = ValueChangeMode.LAZY
                        validateTokens(group, this)
                        addValueChangeListener {
                            val updated = group.copy(tokens = parseTokens(it.value))
                            validateTokens(updated, this)
                            updateGroup(index, updated)
                        }
                    }
                }
            })
        }
    }

    private fun updateGroup(index: Int, group: TokenGroup) {
        tokenGroups = tokenGroups.mapIndexed { i, existing -> if (i == index) group else existing }
        onUpdate(tokenGroups)
    }

    private fun updateGroups(groups: List<TokenGroup>, rerender: Boolean = false) {
        tokenGroups = groups
        onUpdate(groups)
        if (rerender)
            renderGroups()
    }

    private fun parseTokens(input: String): List<String> {
        return input.lines().map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun validateTokens(group: TokenGroup, field: TextArea) {
        val invalidRegex = group.method == TokenMatchMethod.REGEX && group.tokens.any {
            runCatching { it.toRegex() }.isFailure
        }
        field.isInvalid = invalidRegex
        field.errorMessage = if (invalidRegex) "Invalid regex token" else ""
    }
}

private fun TokenTarget.label() = when (this) {
    TokenTarget.BOTH -> "Both"
    TokenTarget.FILE -> "File"
    TokenTarget.RSS -> "RSS"
}

private fun TokenMatchMethod.label() = when (this) {
    TokenMatchMethod.CONTAINS -> "Contains"
    TokenMatchMethod.REGEX -> "Regex"
}

private fun TokenMatchType.label() = when (this) {
    TokenMatchType.ALL -> "All"
    TokenMatchType.ANY -> "Any"
    TokenMatchType.NONE -> "None"
}

@VaadinDsl
fun (@VaadinDsl HasComponents).tokenGroupsComponent(
    tokenGroups: List<TokenGroup>,
    onUpdate: (List<TokenGroup>) -> Unit,
    block: (@VaadinDsl TokenGroupsComponent).() -> Unit = {}
) = init(
    TokenGroupsComponent(tokenGroups, onUpdate), block
)
