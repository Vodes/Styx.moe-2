package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.timepicker.TimePicker
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.theme.lumo.LumoUtility
import com.vaadin.flow.theme.lumo.LumoUtility.*
import moe.styx.db.*
import moe.styx.types.*
import moe.styx.web.data.getTmdbMetadata
import moe.styx.web.getDBClient
import moe.styx.web.getFirstIDFromMap
import moe.styx.web.getFirstTMDBSeason
import org.vaadin.lineawesome.LineAwesomeIcon
import java.time.Duration
import java.time.LocalTime

class MetadataView(private var media: Media, mediaProvider: (Media) -> Media) : KComposite() {
    val root = ui {
        verticalLayout {
            isPadding = false
            h3("Names") { addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Bottom.SMALL, LumoUtility.Padding.Top.MEDIUM) }
            flexLayout {
                setWidthFull()
                maxWidth = "1550px"
                addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL, "flex-container")
                justifyContentMode = FlexComponent.JustifyContentMode.EVENLY

                val nameField = textField("Name") {
                    value = media.name
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { this@MetadataView.media = mediaProvider(media.copy(name = it.value.trim())) }
                }
                val englishField = textField("English") {
                    value = media.nameEN
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { this@MetadataView.media = mediaProvider(media.copy(nameEN = it.value.trim())) }
                }
                val romajiField = textField("Romaji") {
                    value = media.nameJP
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { this@MetadataView.media = mediaProvider(media.copy(nameJP = it.value.trim())) }
                }
                setFlexGrow(1.0, nameField, englishField, romajiField)
            }
            h3("Relations") { addClassNames(Padding.Horizontal.NONE, Padding.Vertical.SMALL) }
            add(RelationsView(media) { mediaProvider(it) })

            h3("Descriptions") { addClassNames(Padding.Horizontal.NONE, Padding.Vertical.SMALL) }
            add(SynopsisView(media) { mediaProvider(it) })

            h3("Misc") { addClassNames(Padding.Horizontal.NONE, Padding.Vertical.SMALL) }
            add(Other(media) { mediaProvider(it) })
        }
    }
}

class RelationsView(private var media: Media, mediaProvider: (Media) -> Media) : KComposite() {
    val root = ui {
        flexLayout {
            addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL, "flex-container")
            setWidthFull()
            maxWidth = "1250px"
            flexLayout {
                setWidthFull()
                addClassNames(LumoUtility.AlignItems.END, LumoUtility.Gap.SMALL)
                val prequelField = textField("Prequel") {
                    value = media.prequel ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { media = mediaProvider(media.copy(prequel = it.value.trim())) }
                    setWidthFull()
                    val tooltip = tooltip.withManual(true)
                    val tooltipButton = Button(LineAwesomeIcon.INFO_SOLID.create())
                    suffixComponent = tooltipButton
                    tooltipButton.onLeftClick {
                        if (value.isNullOrBlank()) {
                            if (tooltip.isOpened)
                                tooltip.isOpened = false
                            return@onLeftClick
                        }
                        if (!tooltip.isOpened) {
                            tooltip.text = getDBClient().executeGet { getMedia(mapOf("GUID" to value)).firstOrNull() }?.name ?: "No media found."
                        }
                        tooltip.isOpened = !tooltip.isOpened;
                    }
                }
                iconButton(LineAwesomeIcon.SEARCH_SOLID.create()) {
                    onLeftClick { MediaChooseDialog(media.GUID) { prequelField.value = it?.GUID ?: "" }.open() }
                    height = prequelField.height
                }
            }
            flexLayout {
                setWidthFull()
                addClassNames(LumoUtility.AlignItems.END, LumoUtility.Gap.SMALL)
                val sequelField = textField("Sequel") {
                    value = media.sequel ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { media = mediaProvider(media.copy(sequel = it.value.trim())) }
                    setWidthFull()
                    val tooltip = tooltip.withManual(true)
                    val tooltipButton = Button(LineAwesomeIcon.INFO_SOLID.create())
                    suffixComponent = tooltipButton
                    tooltipButton.onLeftClick {
                        if (value.isNullOrBlank()) {
                            if (tooltip.isOpened)
                                tooltip.isOpened = false
                            return@onLeftClick
                        }
                        if (!tooltip.isOpened) {
                            tooltip.text = getDBClient().executeGet { getMedia(mapOf("GUID" to value)).firstOrNull() }?.name ?: "No media found."
                        }
                        tooltip.isOpened = !tooltip.isOpened;
                    }
                }
                iconButton(LineAwesomeIcon.SEARCH_SOLID.create()) {
                    onLeftClick { MediaChooseDialog(media.GUID) { sequelField.value = it?.GUID ?: "" }.open() }
                    height = sequelField.height
                }
            }
        }
    }
}

class SynopsisView(private var media: Media, mediaProvider: (Media) -> Media) : KComposite() {
    val root = ui {
        flexLayout {
            addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL, "flex-container")
            setWidthFull()
            maxWidth = "1550px"
            textArea("Synopsis EN") {
                value = media.synopsisEN ?: ""
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener { media = mediaProvider(media.copy(synopsisEN = it.value.trim())) }
                height = "250px"
                setWidthFull()
            }
            val synopsisDE = textArea("Synopsis DE") {
                value = media.synopsisDE ?: ""
                valueChangeMode = ValueChangeMode.LAZY
                addValueChangeListener { media = mediaProvider(media.copy(synopsisDE = it.value.trim())) }
                height = "250px"
                setWidthFull()
            }
            contextMenu {
                target = synopsisDE
                item("Fill from TMDB (Series)") {
                    onLeftClick {
                        val id = media.getFirstIDFromMap(StackType.TMDB)
                        if (id == null)
                            Notification.show("No TMDB ID was found in the mapping.").also { return@onLeftClick }

                        val meta = getTmdbMetadata(id!!, media.isSeries.toBoolean(), "de-DE")
                        if (meta == null)
                            Notification.show("Could not get metadata from TMDB!").also { return@onLeftClick }
                        synopsisDE.value = meta!!.overview
                        media = mediaProvider(media.copy(synopsisDE = synopsisDE.value.trim()))
                    }
                }
                item("Fill from TMDB (Season)") {
                    onLeftClick {
                        val id = media.getFirstIDFromMap(StackType.TMDB)
                        if (id == null)
                            Notification.show("No TMDB ID was found in the mapping.").also { return@onLeftClick }
                        val season = media.getFirstTMDBSeason()
                        if (season == null || !media.isSeries.toBoolean())
                            Notification.show("No season number was found in the mapping.").also { return@onLeftClick }
                        val meta = getTmdbMetadata(id!!, media.isSeries.toBoolean(), "de-DE", season)
                        if (meta == null)
                            Notification.show("Could not get metadata from TMDB!").also { return@onLeftClick }
                        synopsisDE.value = meta!!.overview
                        media = mediaProvider(media.copy(synopsisDE = synopsisDE.value.trim()))
                    }
                }
            }
        }
    }
}

class Other(private var media: Media, mediaProvider: (Media) -> Media) : KComposite() {
    val root = ui {
        val dbClient = getDBClient()
        val categories = dbClient.executeGet(false) { getCategories() }
        var schedule = dbClient.executeGet { getSchedules().find { it.mediaID eqI media.GUID } }
        val weekdays = ScheduleWeekday.entries.toTypedArray()
        verticalLayout {
            isSpacing = false
            isPadding = false
            setWidthFull()
            flexLayout {
                addClassNames(AlignItems.CENTER, Gap.SMALL, Padding.Bottom.MEDIUM, "flex-container")
                setWidthFull()
                maxWidth = "1550px"
                select<Category>("Category") {
                    setItems(categories)
                    isEmptySelectionAllowed = true
                    value = categories.find { it.GUID eqI media.categoryID }
                    setTextRenderer { it.name }
                    addValueChangeListener { media = mediaProvider(media.copy(categoryID = it.value?.GUID ?: "")) }
                    setWidthFull()
                }
                textField("Genres") {
                    value = media.genres ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { media = mediaProvider(media.copy(genres = it.value)) }
                    setWidthFull()
                }
                textField("Tags") {
                    value = media.tags ?: ""
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { media = mediaProvider(media.copy(tags = it.value)) }
                    setWidthFull()
                }
            }
            h3("Schedule") { addClassNames(Padding.Horizontal.NONE, Padding.Vertical.SMALL) }
            flexLayout {
                addClassNames(AlignItems.CENTER, Gap.SMALL, JustifyContent.CENTER, "flex-container")
                setWidthFull()
                maxWidth = "1550px"
                val weekdaySelect = select<ScheduleWeekday>("Weekday") {
                    setItems(*weekdays)
                    isEmptySelectionAllowed = true
                    value = weekdays.find { it == schedule?.day }
                    setTextRenderer { it.toString().lowercase().capitalize() }
                    setWidthFull()
                }
                val time = timePicker("Release Time") {
                    value = LocalTime.of(schedule?.hour ?: 12, schedule?.minute ?: 0)
                    step = Duration.ofMinutes(15)
                    setWidthFull()
                }
                flexLayout {
                    addClassNames(Gap.SMALL, AlignItems.CENTER)
                    val finalEpisodeCount = integerField("Final episode count") {
                        width = "200px"
                        step = 1
                        isStepButtonsVisible = true
                        value = schedule?.finalEpisodeCount ?: 0
                        min = 0
                        valueChangeMode = ValueChangeMode.LAZY
                    }
                    val isEstimated = checkBox("Time estimated") {
                        width = "170px"
                        height = "35px"
                    }
                    setAlignSelf(FlexComponent.Alignment.END, isEstimated)

                    val test = {
                        updateSchedule(media, weekdaySelect, time, isEstimated, finalEpisodeCount) {
                            if (it != null) {
                                getDBClient().executeAndClose { save(it) }
                                schedule = it
                            } else if (schedule != null)
                                getDBClient().executeAndClose { delete(schedule!!) }
                        }
                    }

                    weekdaySelect.addValueChangeListener { test() }
                    time.addValueChangeListener { test() }
                    finalEpisodeCount.addValueChangeListener { test() }
                    isEstimated.addValueChangeListener { test() }
                }
            }
        }
    }
}

fun updateSchedule(
    media: Media,
    weekday: Select<ScheduleWeekday>,
    time: TimePicker,
    estimated: Checkbox,
    countField: IntegerField,
    onReturn: (MediaSchedule?) -> Unit
) {
    if (weekday.isEmpty || weekday.value == null) {
        onReturn(null)
        return
    }
    val schedule = MediaSchedule(media.GUID, weekday.value, time.value.hour, time.value.minute, estimated.value.toInt(), countField.value)
    onReturn(schedule)
}