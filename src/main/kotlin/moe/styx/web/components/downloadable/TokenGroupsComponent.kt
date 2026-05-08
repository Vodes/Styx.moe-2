package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.common.data.TokenGroup
import moe.styx.common.data.TokenMatchMethod
import moe.styx.common.data.TokenMatchType
import moe.styx.common.data.TokenTarget
import moe.styx.common.data.Media
import moe.styx.web.components.addTokenGroupTemplateMenu
import moe.styx.web.createComponent
import org.vaadin.lineawesome.LineAwesomeIcon

class TokenGroupsComponent(
    private val media: Media,
    private var tokenGroups: List<TokenGroup>,
    private val onUpdate: (List<TokenGroup>) -> Unit
) : KComposite() {
    private data class GroupRowState(
        val targetSelect: Select<TokenTarget>,
        val methodSelect: Select<TokenMatchMethod>,
        val matchSelect: Select<TokenMatchType>,
        val tokensField: TextArea
    )

    private lateinit var groupLayout: VerticalLayout
    private val rowStates = mutableMapOf<Int, GroupRowState>()

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
                        updateGroups(readGroups() + TokenGroup(), true)
                    }
                }
                button("Templates") {
                    setTooltipText("Append a preset to the current token groups")
                    addTokenGroupTemplateMenu(media) {
                        updateGroups(readGroups() + it, true)
                    }
                }
            }
        }.also {
            renderGroups()
        }
    }

    private fun renderGroups() {
        groupLayout.removeAll()
        rowStates.clear()
        if (tokenGroups.isEmpty()) {
            groupLayout.add(createComponent { h4("No token groups added yet!") })
            return
        }

        tokenGroups.forEachIndexed { index, group ->
            lateinit var targetSelect: Select<TokenTarget>
            lateinit var methodSelect: Select<TokenMatchMethod>
            lateinit var matchSelect: Select<TokenMatchType>
            lateinit var tokensField: TextArea

            val component = createComponent {
                verticalLayout {
                    setWidthFull()
                    isPadding = false
                    if (index != 0)
                        addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_30, LumoUtility.Padding.Top.MEDIUM)

                    horizontalLayout {
                        defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                        addClassNames(LumoUtility.Gap.SMALL, "flex-container", "token-group-row")
                        setWidthFull()

                        targetSelect = select<TokenTarget>("Target") {
                            setItems(TokenTarget.entries)
                            value = group.target
                            isEmptySelectionAllowed = false
                            setTextRenderer { it.label() }
                            addValueChangeListener {
                                val updated = readGroup(index)
                                validateTokens(updated, tokensField)
                                updateGroup(index, updated)
                            }
                            width = "130px"
                        }
                        methodSelect = select<TokenMatchMethod>("Method") {
                            setItems(TokenMatchMethod.entries)
                            value = group.method
                            isEmptySelectionAllowed = false
                            setTextRenderer { it.label() }
                            addValueChangeListener {
                                val updated = readGroup(index)
                                validateTokens(updated, tokensField)
                                updateGroup(index, updated)
                            }
                            width = "150px"
                        }
                        matchSelect = select<TokenMatchType>("Match") {
                            setItems(TokenMatchType.entries)
                            value = group.matchType
                            isEmptySelectionAllowed = false
                            setTextRenderer { it.label() }
                            addValueChangeListener {
                                val updated = readGroup(index)
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
                                updateGroups(readGroups().filterIndexed { i, _ -> i != index }, true)
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
                            val updated = readGroup(index)
                            validateTokens(updated, this)
                            updateGroup(index, updated)
                        }
                    }
                }
            }
            rowStates[index] = GroupRowState(targetSelect, methodSelect, matchSelect, tokensField)
            groupLayout.add(component)
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

    private fun readGroups(): List<TokenGroup> {
        if (rowStates.isEmpty())
            return tokenGroups

        return tokenGroups.indices.map { index -> readGroup(index) }
    }

    private fun readGroup(index: Int): TokenGroup {
        val state = rowStates[index] ?: return tokenGroups[index]
        return TokenGroup(
            tokens = parseTokens(state.tokensField.value),
            method = state.methodSelect.value,
            matchType = state.matchSelect.value,
            target = state.targetSelect.value,
        )
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
    media: Media,
    tokenGroups: List<TokenGroup>,
    onUpdate: (List<TokenGroup>) -> Unit,
    block: (@VaadinDsl TokenGroupsComponent).() -> Unit = {}
) = init(
    TokenGroupsComponent(media, tokenGroups, onUpdate), block
)
