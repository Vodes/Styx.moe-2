package moe.styx.web.components

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.kaributools.selectionMode
import com.vaadin.flow.component.UI
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
import moe.styx.db.delete
import moe.styx.db.getEntries
import moe.styx.db.getMedia
import moe.styx.types.Media
import moe.styx.types.toBoolean
import moe.styx.web.*
import moe.styx.web.components.media.ImportDialog
import moe.styx.web.views.sub.DownloadableView
import moe.styx.web.views.sub.MediaView
import java.io.File

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
            if (onClickItem != null) {
                addItemClickListener {
                    onClickItem(it.item)
                }
            }
            selectionMode = Grid.SelectionMode.NONE
            setItems(mediaProvider)
            columnFor(Media::name, sortable = true) { setHeader("Name") }
            columnFor(Media::nameEN, sortable = true) { setHeader("English") }
            columnFor(Media::nameJP, sortable = true) { setHeader("Romaji") }
            columnFor(Media::added, sortable = true, converter = { it?.toISODate() ?: "" })
            gridContextMenu {
                isOpenOnClick = onClickItem == null
                item("View", clickListener = {
                    if (it == null) {
                        Notification.show("How did you even manage to do that?")
                        return@item
                    }
                    navigateTo(MediaView::class, it.GUID)
                })

                item("Delete", clickListener = { m ->
                    if (m == null) {
                        Notification.show("How did you even manage to do that?")
                        return@item
                    }
                    val entries = getDBClient().executeGet { getEntries(mapOf("mediaID" to m.GUID)) }
                    val folders = entries.map { File(it.filePath).parentFile }.toSet()
                    ConfirmDialog().apply {
                        setHeader("Do you really want to delete this?")
                        if (m.isSeries.toBoolean())
                            setText(
                                htmlSpan(
                                    "This can free up ${
                                        entries.sumOf { it.fileSize }.readableSize()
                                    } and would delete the following folders:<br>" +
                                            folders.joinToString("<br>") { it.absolutePath }
                                )
                            )
                        else
                            setText("This can free up ${entries.sumOf { it.fileSize }.readableSize()}.")
                        setRejectText("with Files")
                        setRejectable(true)
                        setCancelable(true)
                        isCloseOnEsc = false
                        setConfirmText("Yes")
                        setCancelText("No")

                        addConfirmListener {
                            getDBClient().executeAndClose {
                                delete(m)
                                entries.forEach { delete(it) }
                            }
                            UI.getCurrent().page.reload()
                        }
                        addRejectListener {
                            if (isWindows())
                                return@addRejectListener
                            getDBClient().executeAndClose {
                                delete(m)
                                entries.forEach { delete(it) }
                                if (m.isSeries.toBoolean())
                                    folders.forEach {
                                        if (it.exists())
                                            it.deleteRecursively()
                                    }
                                else
                                    entries.map { File(it.filePath) }.forEach { if (it.exists()) it.delete() }
                            }
                            UI.getCurrent().page.reload()
                        }
                    }.open()
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
                    ImportDialog(it).open()
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