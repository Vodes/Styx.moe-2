package moe.styx.web.components

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.getRouteUrl
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
import com.vaadin.flow.router.QueryParameters
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.common.data.Media
import moe.styx.common.extension.readableSize
import moe.styx.common.extension.toBoolean
import moe.styx.common.isWindows
import moe.styx.common.util.isClose
import moe.styx.db.StyxDBClient
import moe.styx.db.delete
import moe.styx.db.getEntries
import moe.styx.db.getMedia
import moe.styx.web.Main
import moe.styx.web.components.media.ImportDialog
import moe.styx.web.getDBClient
import moe.styx.web.toISODate
import moe.styx.web.views.sub.DownloadableView
import moe.styx.web.views.sub.MediaView
import java.io.File

class MediaGrid(dbClient: StyxDBClient, exclude: String = "", initialSearch: String? = null, onClickItem: ((Media) -> Unit)? = null) : KComposite() {
    private lateinit var searchField: TextField
    private lateinit var movieCheck: Checkbox
    private val media = dbClient.executeGet(false) { getMedia() }.sortedByDescending { it.added }.filter { it.GUID != exclude }
    private val mediaProvider = ListDataProvider(media)

    val root = ui {
        verticalLayout(false, false) {
            addClassNames(LumoUtility.Padding.NONE, LumoUtility.Margin.NONE)

            horizontalLayout {
                addClassNames(LumoUtility.Margin.Vertical.MEDIUM, LumoUtility.Margin.Horizontal.SMALL)
                searchField = textField {
                    placeholder = "Search"
                    valueChangeMode = ValueChangeMode.LAZY
                    value = if (initialSearch.isNullOrBlank()) "" else initialSearch
                }
                if (exclude.isBlank())
                    button("Add new") {
                        onLeftClick { navigateTo(MediaView::class) }
                    }
                movieCheck = checkBox("Movies only")
                searchField.addValueChangeListener {
                    updateFilter(it.value)
                    if (exclude.isBlank()) {
                        val ui = UI.getCurrent()
                        val new = getRouteUrl(
                            ui.currentView::class,
                            queryParameters = if (it.value.isNullOrBlank()) QueryParameters.empty() else QueryParameters.of("q", it.value)
                        )
                        ui.page.history.replaceState(null, new)
                    }
                }
                movieCheck.addValueChangeListener { updateFilter(searchField.value) }
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
                        if (!it.checkValidMedia(true))
                            return@item
                        navigateTo(MediaView::class, it!!.GUID)
                    })
                    item("View in new tab", clickListener = {
                        if (!it.checkValidMedia(true))
                            return@item
                        val route = getRouteUrl(MediaView::class)
                        UI.getCurrent().page.open("${Main.config.baseURL}/$route/${it!!.GUID}")
                    })
                    item("Delete", clickListener = { onDeleteClick(it) })
                    separator()
                    item("Import Episodes", clickListener = {
                        if (!it.checkValidMedia())
                            return@item
                        ImportDialog(it!!).open()
                    })
                    item("Configure Downloader", clickListener = {
                        if (!it.checkValidMedia())
                            return@item
                        navigateTo(DownloadableView::class, it!!.GUID)
                    })
                }
            }
        }.also {
            if (!initialSearch.isNullOrBlank())
                updateFilter(initialSearch)
        }
    }

    fun updateFilter(search: String?) {
        mediaProvider.clearFilters()
        if (movieCheck.value)
            mediaProvider.addFilter { !it.isSeries.toBoolean() }
        if (!search.isNullOrBlank() && search.length > 2) {
            mediaProvider.addFilter { media ->
                media.name.isClose(search) || media.nameEN.isClose(search) || media.nameJP.isClose(search)
            }
        }
    }
}

private fun Media?.checkValidMedia(allowMovie: Boolean = false): Boolean {
    if (this == null) {
        Notification.show("No media was selected.")
        return false
    }
    if (!allowMovie && !this.isSeries.toBoolean()) {
        Notification.show("This is a movie.")
        return false
    }
    return true
}

private fun onDeleteClick(m: Media?) {
    if (!m.checkValidMedia(true))
        return

    val entries = getDBClient().executeGet { getEntries(mapOf("mediaID" to m!!.GUID)) }
    val folders = entries.map { File(it.filePath).parentFile }.toSet()
    ConfirmDialog().apply {
        setHeader("Do you really want to delete this?")
        if (m!!.isSeries.toBoolean())
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
            if (isWindows)
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
}