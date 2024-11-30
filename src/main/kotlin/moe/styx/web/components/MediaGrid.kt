package moe.styx.web.components

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.getRouteUrl
import com.github.mvysny.kaributools.navigateTo
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.confirmdialog.ConfirmDialog
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.GridVariant
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.provider.ListDataProvider
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.QueryParameters
import com.vaadin.flow.shared.Registration
import com.vaadin.flow.theme.lumo.LumoUtility.*
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.data.Media
import moe.styx.common.extension.eqI
import moe.styx.common.extension.readableSize
import moe.styx.common.extension.toBoolean
import moe.styx.common.isWindows
import moe.styx.common.util.isClose
import moe.styx.db.tables.DownloaderTargetsTable
import moe.styx.db.tables.ImageTable
import moe.styx.db.tables.MediaEntryTable
import moe.styx.db.tables.MediaTable
import moe.styx.web.components.media.ImportDialog
import moe.styx.web.dbClient
import moe.styx.web.toISODate
import moe.styx.web.views.sub.DownloadableView
import moe.styx.web.views.sub.MediaView
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import java.io.File

class MediaGrid(exclude: String = "", initialSearch: String? = null, onClickItem: ((Media) -> Unit)? = null) : KComposite() {
    private lateinit var searchField: TextField
    private lateinit var movieCheck: Checkbox
    private lateinit var hasDownloadableCheck: Checkbox
    private lateinit var mediaGrid: Grid<Media>
    private var listener: Registration? = null
    private val media =
        dbClient.transaction { MediaTable.query { selectAll().toList() } }.sortedByDescending { it.added }.filter { it.GUID != exclude }
    private val mediaProvider = ListDataProvider(media)


    override fun onAttach(attachEvent: AttachEvent?) {
        super.onAttach(attachEvent)
        ui.ifPresent {
            listener = it.page.addBrowserWindowResizeListener {
                updateGridColumns(it.width)
            }
            it.page.retrieveExtendedClientDetails {
                updateGridColumns(it.bodyClientWidth)
            }
        }
    }

    override fun onDetach(detachEvent: DetachEvent?) {
        super.onDetach(detachEvent)
        listener?.remove()
    }

    private fun updateGridColumns(width: Int) {
        runCatching {
            if (width > 700) {
                mediaGrid.columns[1].isVisible = true
                mediaGrid.columns[2].isVisible = true
            } else {
                mediaGrid.columns[1].isVisible = false
                mediaGrid.columns[2].isVisible = false
            }
        }
    }

    val root = ui {
        verticalLayout(false, false) {
            addClassNames(Padding.NONE, Margin.NONE)

            verticalLayout(false, false) {
                addClassNames(Margin.Vertical.MEDIUM, Margin.Horizontal.SMALL)
                horizontalLayout(false) {
                    addClassNames(Margin.Vertical.MEDIUM, Margin.Horizontal.SMALL)
                    searchField = textField {
                        placeholder = "Search"
                        valueChangeMode = ValueChangeMode.LAZY
                        value = if (initialSearch.isNullOrBlank()) "" else initialSearch
                    }
                    if (exclude.isBlank())
                        button("Add new") {
                            onClick { navigateTo(MediaView::class) }
                        }
                }
                horizontalLayout(false) {
                    addClassNames(Margin.Vertical.MEDIUM, Margin.Horizontal.SMALL)
                    movieCheck = checkBox("Movies only")
                    hasDownloadableCheck = checkBox("Has download profile")
                }
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
                hasDownloadableCheck.addValueChangeListener { updateFilter(searchField.value) }
            }
            mediaGrid = grid<Media> {
                setWidthFull()
                addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT)
                if (onClickItem != null) {
                    addItemClickListener {
                        onClickItem(it.item)
                    }
                } else {
                    addItemClickListener {
                        MediaClickDialog(it.item).open()
                    }
                }
                selectionMode = Grid.SelectionMode.NONE
                setItems(mediaProvider)
                columnFor(Media::name, sortable = true) {
                    setHeader("Name")
                    setFlexGrow(1)
                }
                columnFor(Media::nameEN, sortable = true) { setHeader("English"); setFlexGrow(1) }
                columnFor(Media::nameJP, sortable = true) { setHeader("Romaji"); setFlexGrow(1) }
                columnFor(Media::added, sortable = true, converter = { it?.toISODate() ?: "" }) { setHeader("Added"); setFlexGrow(1) }
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
        if (hasDownloadableCheck.value) {
            dbClient.transaction {
                val targets = DownloaderTargetsTable.query { selectAll().toList() }
                mediaProvider.addFilter { targets.find { t -> t.mediaID eqI it.GUID } != null }
            }
        }
        if (!search.isNullOrBlank() && search.length > 2) {
            mediaProvider.addFilter { media ->
                media.name.isClose(search) || media.nameEN.isClose(search) || media.nameJP.isClose(search)
            }
        }
    }

    private class MediaClickDialog(private val media: Media) : Dialog() {
        init {
            verticalLayout {
                button("View") {
                    addClassNames(Padding.Vertical.MEDIUM)
                    onClick { navigateTo(MediaView::class, media.GUID); close() }
                }
                button("View in new tab") {
                    addClassNames(Padding.Vertical.MEDIUM)
                    onClick {
                        val route = getRouteUrl(MediaView::class)
                        UI.getCurrent().page.open("${UnifiedConfig.current.base.siteBaseURL()}/$route/${media.GUID}")
                        close()
                    }
                }
                horizontalLayout(padding = false, spacing = false) {
                    addClassNames(Border.BOTTOM, BorderColor.CONTRAST_30, Margin.Bottom.SMALL, Padding.Top.SMALL, Padding.Bottom.MEDIUM)
                    button("Delete") {
                        addClassNames(Padding.NONE, Margin.NONE)
                        onClick {
                            close()
                            onDeleteClick(media)
                        }
                    }
                }
                button("Import Episodes") {
                    addClassNames(Padding.Vertical.MEDIUM)
                    onClick {
                        close()
                        ImportDialog(media).open()
                    }
                }
                button("Configure Downloader") {
                    addClassNames(Padding.Vertical.MEDIUM)
                    onClick { navigateTo(DownloadableView::class, media.GUID); close() }
                }
                button("Configure Downloader in new Tab") {
                    addClassNames(Padding.Vertical.MEDIUM)
                    onClick { UI.getCurrent().page.open("${UnifiedConfig.current.base.siteBaseURL()}/download/${media.GUID}") }
                }
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
    fun deleteMedia(m: Media) {
        val imagesToDelete =
            dbClient.transaction { ImageTable.query { selectAll().where { GUID eq (m.thumbID ?: "") or (GUID eq (m.bannerID ?: "")) }.toList() } }
        dbClient.transaction {
            imagesToDelete.forEach { img -> ImageTable.deleteWhere { GUID eq img.GUID } }
        }
        dbClient.transaction { MediaTable.deleteWhere { GUID eq m.GUID } }
    }

    if (!m.checkValidMedia(true))
        return
    val entries = dbClient.transaction { MediaEntryTable.query { selectAll().where { mediaID eq m!!.GUID }.toList() } }
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
            dbClient.transaction {
                entries.forEach { ent -> MediaEntryTable.deleteWhere { GUID eq ent.GUID } }
            }
            deleteMedia(m)
            UI.getCurrent().page.reload()
        }
        addRejectListener {
            if (isWindows)
                return@addRejectListener
            dbClient.transaction {
                entries.forEach { ent -> MediaEntryTable.deleteWhere { GUID eq ent.GUID } }
                if (m.isSeries.toBoolean())
                    folders.forEach {
                        if (it.exists())
                            it.deleteRecursively()
                    }
                else
                    entries.map { File(it.filePath) }.forEach { if (it.exists()) it.delete() }
            }
            deleteMedia(m)
            UI.getCurrent().page.reload()
        }
    }.open()
}