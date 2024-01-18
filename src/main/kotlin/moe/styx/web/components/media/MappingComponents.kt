package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility
import kotlinx.serialization.encodeToString
import moe.styx.types.*
import moe.styx.web.data.getMalIDForAnilistID
import moe.styx.web.data.tmdb.tmdbFindGroups
import org.vaadin.lineawesome.LineAwesomeIcon

class MappingOverview(private var media: Media, mediaProvider: (Media) -> Media) : KComposite() {
    val root = ui {
        verticalLayout {
            isPadding = false
            isSpacing = false
            val mappingJson = media.metadataMap?.let {
                if (it.isBlank())
                    return@let null
                json.decodeFromString<MappingCollection>(it)
            }
            val tmdbStack = MappingStack(media, StackType.TMDB, mappingJson?.tmdbMappings ?: mutableListOf<TMDBMapping>())
            val anilistStack = MappingStack(media, StackType.ANILIST, mappingJson?.anilistMappings ?: mutableListOf<BasicMapping>())
            val malStack = MappingStack(media, StackType.MAL, mappingJson?.malMappings ?: mutableListOf<BasicMapping>(), anilistStack)
            add(tmdbStack, anilistStack, malStack)
            button("Save") {
                addThemeVariants(ButtonVariant.LUMO_SUCCESS)
                onLeftClick {
                    val mappings = MappingCollection(
                        tmdbStack.getMappings() as MutableList<TMDBMapping>,
                        anilistStack.getMappings() as MutableList<BasicMapping>,
                        malStack.getMappings() as MutableList<BasicMapping>
                    )
                    media = mediaProvider(media.copy(metadataMap = json.encodeToString(mappings)))
                }
            }
        }
    }
}

class MappingStack(val media: Media, val type: StackType, private val mappings: List<IMapping>, private val anilistStack: MappingStack? = null) :
    KComposite() {
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
                        addEntry(StackEntry(this@MappingStack, if (type == StackType.TMDB) TMDBMapping() else BasicMapping()))
                    }
                }
                if (type == StackType.MAL) {
                    iconButton(LineAwesomeIcon.SYNC_SOLID.create()) {
                        setTooltipText("Sync with Anilist Stack (converting IDs)")
                        onLeftClick {
                            if (anilistStack == null || anilistStack.entries.isEmpty())
                                return@onLeftClick
                            entries.clear()
                            entryLayout.removeAll()
                            anilistStack.entries.forEach {
                                val id = it.mappingEntry.remoteID
                                val newID = getMalIDForAnilistID(id)
                                if (newID != null)
                                    addEntry(StackEntry(this@MappingStack, (it.mappingEntry as BasicMapping).copy(remoteID = newID)))
                            }
                        }
                    }
                } else {
                    iconButton(LineAwesomeIcon.SEARCH_SOLID.create()) {
                        onLeftClick {
                            MappingSearchDialog(this@MappingStack, media).open()
                        }
                    }
                }
            }
            entryLayout = verticalLayout {}
        }.also {
            mappings.forEach {
                val stackEnt = StackEntry(this@MappingStack, it)
                entries.add(stackEnt)
                entryLayout.add(stackEnt)
            }
        }
    }

    fun getMappings() = entries.map { it.mappingEntry }

    fun addEntry(entry: StackEntry) {
        entries.add(entry)
        entryLayout.removeAll()
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
}

class StackEntry(parent: MappingStack, var mappingEntry: IMapping) : KComposite() {
    private lateinit var idMappingField: TextField
    private lateinit var seasonField: IntegerField

    val root = ui {
        flexLayout {
            setWidthFull()
            addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL, "flex-container")
            numberField("Match From") {
                value = mappingEntry.matchFrom
                step = 0.5
                isStepButtonsVisible = true
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener { mappingEntry.matchFrom = it.value }
            }
            numberField("Match Until") {
                value = mappingEntry.matchUntil
                step = 0.5
                isStepButtonsVisible = true
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener { mappingEntry.matchUntil = it.value }
            }
            idMappingField = textField("ID") {
                value = mappingEntry.remoteID.toString()
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener { mappingEntry.remoteID = it.value.toIntOrNull() ?: 0 }
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
                value = (mappingEntry as? TMDBMapping)?.seasonEntry ?: 1
                min = 0
                step = 1
                isStepButtonsVisible = true
                isVisible = mappingEntry is TMDBMapping && parent.media.isSeries.toBoolean()
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener {
                    if (mappingEntry is TMDBMapping)
                        (mappingEntry as TMDBMapping).seasonEntry = it.value
                }
            }
            horizontalLayout {
                isPadding = false
                addClassNames(LumoUtility.Gap.SMALL)
                select<TMDBOrder>("Episode Order") {
                    setItems(TMDBOrder.entries)
                    isEmptySelectionAllowed = true
                    value = (mappingEntry as? TMDBMapping)?.orderType
                    isVisible = mappingEntry is TMDBMapping && parent.media.isSeries.toBoolean()
                    setTextRenderer { it.title }
                    addValueChangeListener { event ->
                        if (mappingEntry !is TMDBMapping || idMappingField.value.isNullOrBlank())
                            return@addValueChangeListener

                        if (event.value == null) {
                            (mappingEntry as TMDBMapping).orderType = null
                            (mappingEntry as TMDBMapping).orderID = null
                            return@addValueChangeListener
                        }

                        val groups = idMappingField.value.toIntOrNull()?.let { tmdbFindGroups(it) }
                        if (groups == null || groups.results.isNullOrEmpty()) {
                            Notification.show("Could not find any episode groups for this ID!", 1200, Notification.Position.TOP_CENTER)
                            return@addValueChangeListener
                        }
                        val matchingGroups = groups.results.filter { it.type == event.value.type }
                        if (matchingGroups.isEmpty()) {
                            Notification.show("Could not find a group for this type!", 1200, Notification.Position.TOP_CENTER)
                            return@addValueChangeListener
                        }
                        val selectedGroup = if (event.value == TMDBOrder.ABSOLUTE)
                            matchingGroups.minByOrNull { it.groupCount }
                        else
                            matchingGroups.firstOrNull()

                        if (selectedGroup == null) {
                            Notification.show("Could not find a group for this type!", 1200, Notification.Position.TOP_CENTER)
                            return@addValueChangeListener
                        }

                        (mappingEntry as TMDBMapping).orderType = event.value
                        (mappingEntry as TMDBMapping).orderID = selectedGroup.id
                        val notification = Notification(Span("Selected group \"${selectedGroup.name}\"!"), Button("Show").apply {
                            onLeftClick {
                                UI.getCurrent().page.open("https://www.themoviedb.org/tv/${idMappingField.value}/episode_group/${selectedGroup.id}")
                            }
                        })
                        notification.duration = 1500
                        notification.position = Notification.Position.TOP_CENTER
                        notification.open()
                    }
                }
                numberField("Episode Offset") {
                    value = mappingEntry.offset
                    step = 0.5
                    isStepButtonsVisible = true
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { mappingEntry.offset = it.value }
                }
                iconButton(LineAwesomeIcon.TRASH_SOLID.create()) {
                    onLeftClick {
                        parent.removeEntry(this@StackEntry)
                    }
                }
            }
        }
    }
}

enum class StackType(val displayName: String, val key: String) {
    ANILIST("Anilist", "anilist"),
    TMDB("TMDB", "tmdb"),
    MAL("MyAnimeList", "mal")
}