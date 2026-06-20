package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.notification.Notification
import kotlinx.serialization.encodeToString
import moe.styx.common.data.*
import moe.styx.common.data.tmdb.decodeMapping
import moe.styx.common.json
import moe.styx.db.tables.ChangesTable
import moe.styx.db.tables.MediaEntryTable
import moe.styx.db.tables.MediaTable
import moe.styx.web.dbClient
import moe.styx.web.topNotification
import moe.styx.web.util.mergeMappings
import moe.styx.web.util.offsetEpisode
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class MergeDialog(private val target: Media, private val donor: Media? = null) : Dialog() {
    private val targetEntries = dbClient.transaction { entriesFor(target) }.sortedByEpisode()

    init {
        setSizeFull()
        maxWidth = "900px"
        maxHeight = "700px"

        verticalLayout {
            setSizeFull()
            h2("Merge media into ${target.name}")
            p("The donor media row is deleted after merging, so media-level favourites, preferences, schedules and downloader configuration for the donor will be removed by database cascade.")

            button("Select donor media") {
                onClick {
                    MediaChooseDialog(target.GUID) {
                        close()
                        if (it != null)
                            MergeDialog(target, it).open()
                    }.open()
                }
            }

            val selectedDonor = this@MergeDialog.donor
            if (selectedDonor != null) {
                val donorEntries = dbClient.transaction { entriesFor(selectedDonor) }.sortedByEpisode()
                val episodeOffset = targetEntries.size
                h3("Preview")
                p("${selectedDonor.name}: ${donorEntries.size} episodes will become episodes ${episodeOffset + 1}-${episodeOffset + donorEntries.size}.")
                grid<MergePreview> {
                    setWidthFull()
                    setItems(donorEntries.map { entry ->
                        MergePreview(entry.entryNumber, offsetEpisode(entry.entryNumber, episodeOffset) ?: "Invalid")
                    })
                    columnFor(MergePreview::from) { setHeader("Donor episode") }
                    columnFor(MergePreview::to) { setHeader("Merged episode") }
                }
                horizontalLayout {
                    button("Cancel") {
                        onClick { close() }
                    }
                    button("Merge") {
                        addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR)
                        onClick {
                            merge(selectedDonor, donorEntries)
                        }
                    }
                }
            }
        }
    }

    private fun merge(donor: Media, donorEntries: List<MediaEntry>) {
        if (donor.GUID == target.GUID) {
            topNotification("Cannot merge media into itself.")
            return
        }
        if (donorEntries.isEmpty()) {
            topNotification("Donor media has no entries.")
            return
        }

        val episodeOffset = targetEntries.size
        val movedEntries = donorEntries.map { entry ->
            val newNumber = offsetEpisode(entry.entryNumber, episodeOffset)
            if (newNumber == null) {
                topNotification("Could not offset donor episode ${entry.entryNumber}.")
                return
            }
            entry.copy(mediaID = target.GUID, entryNumber = newNumber)
        }

        val mergedMappings = mergeMappings(
            target.decodeMapping(),
            donor.decodeMapping(),
            targetEntries.size,
            donorEntries.size
        )

        dbClient.transaction {
            movedEntries.forEach(MediaEntryTable::upsertItem)
            MediaTable.upsertItem(target.copy(metadataMap = json.encodeToString(mergedMappings)))
            MediaTable.deleteWhere { GUID eq donor.GUID }
            ChangesTable.setToNow(true, true)
        }
        Notification.show("Merged ${donor.name} into ${target.name}.", 2500, Notification.Position.TOP_CENTER)
        close()
    }
}

private data class MergePreview(val from: String, val to: String)

private fun entriesFor(media: Media): List<MediaEntry> =
    MediaEntryTable.query { selectAll().where { mediaID eq media.GUID }.toList() }

private fun List<MediaEntry>.sortedByEpisode(): List<MediaEntry> =
    sortedWith(compareBy<MediaEntry> { it.entryNumber.toDoubleOrNull() ?: Double.MAX_VALUE }.thenBy { it.entryNumber })
