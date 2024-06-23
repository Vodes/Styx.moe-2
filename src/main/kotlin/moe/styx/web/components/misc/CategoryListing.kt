package moe.styx.web.components.misc

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.selectionMode
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.data.provider.ListDataProvider
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility.Padding
import moe.styx.common.data.Category
import moe.styx.common.extension.toBoolean
import moe.styx.common.extension.toInt
import moe.styx.db.tables.CategoryTable
import moe.styx.db.tables.MediaTable
import moe.styx.web.createComponent
import moe.styx.web.dbClient
import moe.styx.web.newGUID
import moe.styx.web.topNotification
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager

val Category.visibleString
    get() = if (isVisible.toBoolean()) "Yes" else "No"

val Category.seriesString
    get() = if (isSeries.toBoolean()) "Yes" else "No"

fun categoryListing(readonly: Boolean) = createComponent {
    verticalLayout {
        if (readonly)
            nativeLabel("Due to lacking permissions, this view is in readonly mode.")

        val items = dbClient.transaction { CategoryTable.query { selectAll().toList() } }.sortedByDescending { it.sort }
        button("Add new") {
            onClick {
                if (readonly)
                    return@onClick
                CategoryDialog(null, items).open()
            }
        }

        val provider = ListDataProvider(items)
        grid<Category> {
            setItems(provider)
            setWidthFull()
            minWidth = "400px"
            selectionMode = Grid.SelectionMode.NONE
            addItemClickListener { if (!readonly) CategoryDialog(it.item, items).open() }
            addColumn(Category::name).setHeader("Name").setFlexGrow(1).setSortable(true)
            addColumn(Category::sort).setHeader("Sort").setSortable(true)
            addColumn(Category::seriesString).setHeader("Series Only").setSortable(true)
            addColumn(Category::visibleString).setHeader("Visible").setSortable(true)
        }
    }
}

class CategoryDialog(private var initial: Category? = null, private val categories: List<Category>) : Dialog() {
    private var category = initial ?: Category(newGUID(), 0, 1, 1, "")
    private lateinit var categorySelect: Select<Category>

    init {
        setWidthFull()
        maxWidth = "450px"
        verticalLayout {
            defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            h3(if (initial == null) "New category" else "Editing ${category.name}") {
                addClassNames(Padding.Bottom.MEDIUM)
            }
            textField("Name") {
                value = category.name
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener { category = category.copy(name = it.value) }
                setWidthFull()
            }
            integerField("Sort") {
                setWidthFull()
                step = 1
                isStepButtonsVisible = true
                value = category.sort
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener { category = category.copy(sort = it.value) }
            }
            checkBox("Is Visible") {
                alignSelf = FlexComponent.Alignment.START
                value = category.isVisible.toBoolean()
                addValueChangeListener { category = category.copy(isVisible = it.value.toInt()) }
            }
            checkBox("Series only") {
                alignSelf = FlexComponent.Alignment.START
                value = category.isSeries.toBoolean()
                addValueChangeListener { category = category.copy(isSeries = it.value.toInt()) }
            }

            button("Save") {
                onClick {
                    val result = dbClient.transaction { CategoryTable.upsertItem(category).insertedCount.toBoolean() }
                    if (!result) {
                        topNotification("Could not save category!")
                    } else {
                        UI.getCurrent().page.reload()
                    }
                }
            }

            horizontalLayout(padding = false) {
                defaultVerticalComponentAlignment = FlexComponent.Alignment.END
                button("Delete") {
                    isEnabled = initial != null
                    addThemeVariants(ButtonVariant.LUMO_ERROR)
                    onClick {
                        if (categorySelect.isEmpty || categorySelect.value == null) {
                            topNotification("You need to decide where to move existing shows into.")
                            return@onClick
                        }
                        dbClient.transaction {
                            val media = MediaTable.query { selectAll().where { categoryID eq category.GUID }.toList() }
                            var successful = true
                            for (m in media) {
                                runCatching {
                                    val result = MediaTable.upsertItem(m.copy(categoryID = categorySelect.value.GUID)).insertedCount.toBoolean()
                                    if (!result)
                                        successful = false
                                }.onFailure {
                                    it.printStackTrace()
                                    successful = false
                                }
                                if (!successful)
                                    break
                            }
                            if (!successful) {
                                topNotification("Could not fully update all media entries!")
                                TransactionManager.current().rollback()
                                return@transaction
                            }
                            val deleted = CategoryTable.deleteWhere { GUID eq category.GUID }.toBoolean()
                            if (!deleted) {
                                topNotification("Could not delete category!")
                                TransactionManager.current().rollback()
                            } else {
                                UI.getCurrent().page.reload()
                            }
                        }
                    }
                }
                categorySelect = select("Move media into") {
                    setItems(categories.filter { it.GUID != category.GUID })
                    setItemLabelGenerator { it.name }
                }
            }
        }
    }
}