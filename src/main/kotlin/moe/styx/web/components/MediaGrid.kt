package moe.styx.web.components

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.kaributools.selectionMode
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.confirmdialog.ConfirmDialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.GridVariant
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.provider.ListDataProvider
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.db.StyxDBClient
import moe.styx.db.getMedia
import moe.styx.types.Media
import moe.styx.types.toBoolean
import moe.styx.web.createComponent
import moe.styx.web.toISODate
import moe.styx.web.views.sub.DownloadableView
import moe.styx.web.views.sub.MediaView

fun initMediaComponent(dbClient: StyxDBClient, exclude: String = "", onClickItem: ((Media) -> Unit)? = null) = createComponent {
    val media = dbClient.executeGet(false) { getMedia() }.sortedByDescending { it.added }.filter { it.GUID != exclude }
    val mediaProvider = ListDataProvider(media)
    verticalLayout {
        isSpacing = false
        isPadding = false
        addClassNames(LumoUtility.Padding.NONE, LumoUtility.Margin.NONE)

        horizontalLayout {
            addClassNames(LumoUtility.Margin.Vertical.MEDIUM, LumoUtility.Margin.Horizontal.SMALL)
            val searchField = textField {
                placeholder = "Search"
                valueChangeMode = ValueChangeMode.LAZY
            }
            if (exclude.isBlank())
                button("Add new") {
                    onLeftClick { navigateTo(MediaView::class) }
                }
            val movieCheck = checkBox("Movies only")
            searchField.addValueChangeListener { updateFilter(mediaProvider, searchField, movieCheck) }
            movieCheck.addValueChangeListener { updateFilter(mediaProvider, searchField, movieCheck) }
        }
        grid<Media> {
            minWidth = "750px"
            addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT)
            addItemClickListener {
                if (onClickItem != null) {
                    onClickItem(it.item)
                    return@addItemClickListener
                }
                navigateTo(MediaView::class, it.item.GUID)
            }
            selectionMode = Grid.SelectionMode.NONE
            setItems(mediaProvider)
            columnFor(Media::name, sortable = true) { setHeader("Name") }
            columnFor(Media::nameEN, sortable = true) { setHeader("English") }
            columnFor(Media::nameJP, sortable = true) { setHeader("Romaji") }
            columnFor(Media::added, sortable = true, converter = { it?.toISODate() ?: "" })
            gridContextMenu {
                item("View", clickListener = {
                    if (it == null) {
                        Notification.show("How did you even manage to do that?")
                        return@item
                    }
                    navigateTo(MediaView::class, it.GUID)
                })

                item("Delete", clickListener = {
                    if (it == null) {
                        Notification.show("How did you even manage to do that?")
                        return@item
                    }
                    ConfirmDialog().open()
                })

                separator()
                item("Import Episodes", clickListener = {
                    if (it == null) {
                        Notification.show("How did you even manage to do that?")
                        return@item
                    }
                    if (!it.isSeries.toBoolean()) {
                        Notification.show("This is a movie.")
                        return@item
                    }
                })
                item("Configure Downloader", clickListener = {
                    if (it == null) {
                        Notification.show("How did you even manage to do that?")
                        return@item
                    }
                    if (!it.isSeries.toBoolean()) {
                        Notification.show("This is a movie.")
                        return@item
                    }
                    navigateTo(DownloadableView::class, it.GUID)
                })
            }
        }
    }
}

private fun updateFilter(provider: ListDataProvider<Media>, searchField: TextField, movieCheck: Checkbox) {
    provider.clearFilters()
    if (movieCheck.value)
        provider.addFilter { !it.isSeries.toBoolean() }
    if (!searchField.value.isNullOrBlank() && searchField.value.length > 2)
        provider.addFilter { media ->
            media.name.contains(searchField.value, true)
                    || media.nameEN?.contains(searchField.value, true) ?: false
                    || media.nameJP?.contains(searchField.value, true) ?: false
        }
}