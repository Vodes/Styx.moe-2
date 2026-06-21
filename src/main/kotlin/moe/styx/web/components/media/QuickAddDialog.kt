package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.data.value.ValueChangeMode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import moe.styx.common.data.BasicMapping
import moe.styx.common.data.MappingCollection
import moe.styx.common.data.Media
import moe.styx.common.data.TMDBMapping
import moe.styx.common.extension.toBoolean
import moe.styx.common.json
import moe.styx.common.util.isClose
import moe.styx.web.anilistClient
import moe.styx.web.data.getMalIDForAnilistID
import moe.styx.web.data.tmdb.tmdbFindMedia
import moe.styx.web.topNotification
import moe.styx.web.util.anyTitle
import moe.styx.web.util.cleanedDescription
import moe.styx.web.util.genresString
import moe.styx.web.util.tagsString
import pw.vodes.anilistkmp.ext.fetchMediaByID

class QuickAddDialog(media: Media, val onChoose: (Media) -> Unit) : Dialog() {

    private lateinit var resultLayout: VerticalLayout
    private lateinit var idField: IntegerField

    init {
        setWidthFull()
        maxWidth = "600px"
        h2("Search AniList")
        verticalLayout {
            setWidthFull()
            horizontalLayout {
                setWidthFull()
                defaultVerticalComponentAlignment = FlexComponent.Alignment.END
                idField = integerField("ID")
                button("Use") {
                    onClick {
                        if (idField.value == null) {
                            topNotification("No ID given.")
                            return@onClick
                        }
                        val meta = runBlocking { anilistClient.fetchMediaByID(idField.value).data }
                        if (meta == null) {
                            topNotification("Could not get metadata for this ID.")
                            return@onClick
                        }
                        val malID = getMalIDForAnilistID(meta.id)
                        val tmdbResult = tmdbFindMedia(meta.anyTitle(), media.isSeries.toBoolean()).filter { it.name.isClose(meta.anyTitle()) }
                        val metadataMap = runCatching { json.decodeFromString<MappingCollection>(media.metadataMap!!) }.getOrNull()?.apply {
                            this.anilistMappings.add(BasicMapping(remoteID = meta.id))
                            if (malID != null)
                                this.malMappings.add(BasicMapping(remoteID = malID))
                            if (tmdbResult.isNotEmpty())
                                this.tmdbMappings.add(TMDBMapping(remoteID = tmdbResult.first().id))
                        } ?: MappingCollection(
                            anilistMappings = mutableListOf(BasicMapping(remoteID = meta.id)),
                            malMappings = mutableListOf<BasicMapping>().apply {
                                if (malID != null)
                                    add(BasicMapping(remoteID = malID))
                            }, tmdbMappings = mutableListOf<TMDBMapping>().apply {
                                if (tmdbResult.isNotEmpty())
                                    add(TMDBMapping(remoteID = tmdbResult.first().id))
                            }
                        )
                        onChoose(
                            media.copy(
                                name = meta.anyTitle(),
                                nameEN = meta.title?.english ?: "",
                                nameJP = meta.title?.romaji ?: "",
                                synopsisEN = meta.cleanedDescription ?: "",
                                genres = meta.genresString(),
                                tags = meta.tagsString(),
                                metadataMap = json.encodeToString(metadataMap)
                            )
                        )
                        close()
                    }
                }
            }

            textField("Search") {
                setWidthFull()
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener { changeEvent ->
                    updateAnilistResults(changeEvent.value, resultLayout) {
                        idField.value = it.id
                    }
                }
            }
            resultLayout = verticalLayout {
                setWidthFull()
            }
        }
    }
}
