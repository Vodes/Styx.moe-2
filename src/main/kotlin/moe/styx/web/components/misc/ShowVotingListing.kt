package moe.styx.web.components.misc

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setPrimary
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.AnchorTarget
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.data.provider.ListDataProvider
import kotlinx.coroutines.runBlocking
import moe.styx.common.data.*
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.common.json
import moe.styx.common.util.Log
import moe.styx.db.tables.CategoryTable
import moe.styx.db.tables.ImageTable
import moe.styx.db.tables.MediaTable
import moe.styx.db.tables.ShowVotingTable
import moe.styx.web.*
import moe.styx.web.data.getMalIDForAnilistID
import moe.styx.web.data.tmdb.tmdbFindMedia
import moe.styx.web.util.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import pw.vodes.anilistkmp.ext.fetchMediaByID

val ShowVoting.result
    get() = if (this.votes >= 2) "✓" else (if (this.hasVeto) "✓✓" else "❌")


fun showVotingListing() = createComponent {
    lateinit var categorySelect: Select<Category>
    lateinit var showVotingGrid: Grid<ShowVoting>
    lateinit var addShowsButton: Button
    lateinit var deleteSelectedButton: Button

    verticalLayout {
        val categories = dbClient.transaction { CategoryTable.query { selectAll().toList() }.sortedByDescending { it.sort } }
        val votings = dbClient.transaction { ShowVotingTable.query { selectAll().toList() } }

        horizontalLayout {
            alignItems = FlexComponent.Alignment.END
            categorySelect = select("Category") {
                setItems(categories)
                isEmptySelectionAllowed = true
                setTextRenderer { it.name }
                flexGrow = 1.0
                maxWidth = "200px"
                addValueChangeListener {
                    addShowsButton.isEnabled = it.value != null && showVotingGrid.selectedItems.isNotEmpty()
                }
            }
            addShowsButton = button("Add selected") {
                isEnabled = false
                setPrimary()
                onClick {
                    val results = showVotingGrid.selectedItems.map { addShowFromVoting(it, categorySelect.value) }
                    if (results.any())
                        UI.getCurrent().page.reload()
                }
            }
            deleteSelectedButton = button("Delete selected") {
                isEnabled = false
                addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR)
                onClick {
                    if (showVotingGrid.selectedItems.isEmpty())
                        return@onClick
                    val result = dbClient.transaction {
                        ShowVotingTable.deleteWhere { anilistID inList showVotingGrid.selectedItems.map { it.anilistID } }
                    }
                    if (result > 0)
                        UI.getCurrent().page.reload()
                    else
                        topNotification("Failed to delete selected votings!")
                }
            }
        }

        val votingProvider = ListDataProvider(votings)
        showVotingGrid = grid<ShowVoting> {
            setItems(votingProvider)
            setWidthFull()
            minWidth = "400px"
            selectionMode = Grid.SelectionMode.MULTI
            addSelectionListener {
                addShowsButton.isEnabled = it.allSelectedItems.isNotEmpty() && categorySelect.value != null
                deleteSelectedButton.isEnabled = it.allSelectedItems.isNotEmpty()
            }
            addColumn(ShowVoting::title).setHeader("Name").setFlexGrow(1).setSortable(true)
            addComponentColumn { voting ->
                Anchor("https://anilist.co/anime/${voting.anilistID}", voting.anilistID.toString()).apply {
                    setTarget(AnchorTarget.BLANK)
                }
            }.setHeader("AniList").setSortable(true)
            addColumn(ShowVoting::result).setHeader("Result").setSortable(true)
        }
    }
}

private fun addShowFromVoting(showVoting: ShowVoting, category: Category): Boolean {
    val anilistData = runBlocking { anilistClient.fetchMediaByID(showVoting.anilistID) }
    if (anilistData.data == null || !anilistData.errors.isNullOrEmpty() || anilistData.exception != null) {
        Log.e(exception = anilistData.exception) {
            "Could not fetch anilist data for ID: ${showVoting.anilistID}\n${anilistData.errors?.joinToString { it.message }}"
        }
        return false
    }
    runCatching {
        val malID = getMalIDForAnilistID(showVoting.anilistID)
        val tmdbResult = tmdbFindMedia(anilistData.data!!.anyTitleNoSeason())
        val mappings = MappingCollection(
            anilistMappings = mutableListOf(BasicMapping(remoteID = anilistData.data!!.id)),
            malMappings = mutableListOf<BasicMapping>().apply {
                if (malID != null)
                    add(BasicMapping(remoteID = malID))
            }, tmdbMappings = mutableListOf<TMDBMapping>().apply {
                if (tmdbResult.isNotEmpty())
                    add(TMDBMapping(remoteID = tmdbResult.first().id))
            }
        )
        val image = anilistData.data!!.coverImageURL()?.let { downloadImageForStyx(it, true) }
        val media = Media(
            newGUID(),
            name = anilistData.data!!.anyTitle(),
            nameEN = anilistData.data!!.title?.english ?: "",
            nameJP = anilistData.data!!.title?.romaji ?: "",
            synopsisEN = anilistData.data!!.cleanedDescription ?: "",
            synopsisDE = null,
            thumbID = image?.GUID,
            genres = anilistData.data!!.genresString(),
            tags = anilistData.data!!.tagsString(),
            metadataMap = json.encodeToString(mappings),
            categoryID = category.GUID,
            added = currentUnixSeconds()
        )
        if (image != null) {
            dbClient.transaction { ImageTable.upsertItem(image) }
        }
        val mediaAdded = dbClient.transaction { MediaTable.upsertItem(media).insertedCount == 1 }
        if (mediaAdded) {
            dbClient.transaction {
                ShowVotingTable.deleteWhere { anilistID eq anilistData.data!!.id }
            }
        }
        return mediaAdded
    }.onFailure {
        Log.e(exception = it) { "Failed to add media for anilist ID: ${showVoting.anilistID}" }
    }

    return false
}
