package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.NumberField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.theme.lumo.LumoUtility
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.styx.types.Media
import moe.styx.types.json
import moe.styx.types.toBoolean
import org.vaadin.lineawesome.LineAwesomeIcon

class MappingOverview(private var media: Media, mediaProvider: (Media) -> Media) : KComposite() {
    val root = ui {
        verticalLayout {
            isPadding = false
            isSpacing = false
            val mappingJson = media.metadataMap?.let {
                if (it.isBlank())
                    return@let null
                json.decodeFromString<JsonObject>(it)
            }
            val tmdbStack = MappingStack(media, StackType.TMDB, mappingJson?.get(StackType.TMDB.key)?.jsonObject)
            val anilistStack = MappingStack(media, StackType.ANILIST, mappingJson?.get(StackType.ANILIST.key)?.jsonObject)
            val malStack = MappingStack(media, StackType.MAL, mappingJson?.get(StackType.MAL.key)?.jsonObject)
            add(tmdbStack, anilistStack, malStack)
            button("Save") {
                addThemeVariants(ButtonVariant.LUMO_SUCCESS)
                onLeftClick {
                    val combinedMap = mapOf(
                        tmdbStack.type.key to tmdbStack.generateMap(),
                        anilistStack.type.key to anilistStack.generateMap(),
                        malStack.type.key to malStack.generateMap()
                    )
                    if (combinedMap.filterValues { it.isNotEmpty() }.isEmpty())
                        mediaProvider(media.copy(metadataMap = ""))
                    else
                        mediaProvider(media.copy(metadataMap = json.encodeToString(combinedMap)))
                }
            }
        }
    }
}

class MappingStack(val media: Media, val type: StackType, val jsonObj: JsonObject?) : KComposite() {
    val entries = mutableListOf<StackEntry>()
    lateinit var entryLayout: VerticalLayout

    val root = ui {
        verticalLayout {
            horizontalLayout {
                isPadding = false
                defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                h3(type.displayName)
                iconButton(LineAwesomeIcon.PLUS_SOLID.create()) {
                    onLeftClick {
                        entries.add(
                            StackEntry(this@MappingStack, "", "", "", "", "")
                        )
                        entryLayout.removeAll()
                        entries.forEach {
                            entryLayout.add(it)
                        }
                    }
                }
            }
            entryLayout = verticalLayout {}
        }.also {
            initEntries()
        }
    }

    private fun initEntries() {
        if (jsonObj == null)
            return
        entryLayout.removeAll()
        entries.clear()
        jsonObj.entries.forEach {
            val content = it.value.jsonPrimitive.content
            val offset = content.split("/").getOrNull(1)?.trim()
            var splitID = if (content.contains("/")) content.split("/")[0].split(",") else content.split(",")
            splitID = splitID.map { str -> str.trim() }
            val entry = StackEntry(this, it.key, splitID.getOrNull(0), splitID.getOrNull(1), splitID.getOrNull(2), offset)
            entries.add(entry)
        }
        entries.forEach {
            entryLayout.add(it)
        }
    }

    fun removeEntry(entry: StackEntry) {
        entries.remove(entry)
        entryLayout.removeAll()
        entries.forEach {
            entryLayout.add(it)
        }
    }

    fun generateMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (entry in entries) {
            if (entry.idMappingField.value.isNullOrBlank())
                continue
            val key = entry.epMappingField.value
            var value = entry.idMappingField.value
            if (type == StackType.TMDB) {
                value += ",${entry.seasonField.value}"
                if (!entry.groupField.value.isNullOrBlank()) {
                    value += ",${entry.groupField.value}"
                }
            }
            if (entry.offsetField.value != 0.0)
                value += "/${entry.offsetField.value}"
            map[key] = value
        }
        return map.toMap()
    }
}

class StackEntry(parent: MappingStack, epMapping: String?, idMapping: String?, season: String?, group: String?, offset: String?) : KComposite() {
    lateinit var epMappingField: TextField
    lateinit var idMappingField: TextField
    lateinit var seasonField: IntegerField
    lateinit var groupField: TextField
    lateinit var offsetField: NumberField

    val root = ui {
        flexLayout {
            setWidthFull()
            addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL, "flex-container")
            epMappingField = textField("EP Mapping") {
                placeholder = "e. g. 01-12"
                value = if (epMapping.isNullOrBlank()) "" else epMapping
            }
            idMappingField = textField("ID") {
                value = if (idMapping.isNullOrBlank()) "" else idMapping
            }
            contextMenu {
                target = idMappingField
                item("Open Site", {
                    if (idMappingField.value.isNullOrBlank())
                        Notification.show("No ID given!").also { return@item }
                    when (parent.type) {
                        StackType.TMDB -> {
                            val tv = parent.media.isSeries.toBoolean()
                            var url = "https://www.themoviedb.org/${if (tv) "tv" else "movie"}/${idMappingField.value.trim()}"
                            if (tv)
                                url += "/season/${seasonField.value}"
                            UI.getCurrent().page.open(url)
                        }

                        StackType.MAL -> UI.getCurrent().page.open("https://myanimelist.net/anime/${idMappingField.value.trim()}")
                        else -> UI.getCurrent().page.open("https://anilist.co/anime/${idMappingField.value.trim()}")
                    }
                })
            }

            seasonField = integerField("Season") {
                value = season?.toIntOrNull() ?: 1
                min = 0
                step = 1
                isStepButtonsVisible = true
            }
            horizontalLayout {
                isPadding = false
                addClassNames(LumoUtility.Gap.SMALL)
                groupField = textField("Episode Group") {
                    value = if (group.isNullOrBlank()) "" else group
                }
                offsetField = numberField("Episode Offset") {
                    value = offset?.toDoubleOrNull() ?: 0.0
                    step = 0.5
                    isStepButtonsVisible = true
                }
                if (parent.entries.isNotEmpty() && parent.entries[0] != this@StackEntry)
                    iconButton(LineAwesomeIcon.TRASH_SOLID.create()) {
                        onLeftClick {
                            parent.removeEntry(this@StackEntry)
                        }
                    }
            }

            if (parent.type != StackType.TMDB || !parent.media.isSeries.toBoolean()) {
                groupField.isVisible = false
                seasonField.isVisible = false
            }
        }
    }
}

enum class StackType(val displayName: String, val key: String) {
    ANILIST("Anilist", "anilist"),
    TMDB("TMDB", "tmdb"),
    MAL("MyAnimeList", "mal")
}