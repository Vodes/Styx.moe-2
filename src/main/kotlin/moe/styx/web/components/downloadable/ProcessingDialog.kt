package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.data.value.ValueChangeMode
import moe.styx.common.data.ProcessingOptions
import moe.styx.web.components.LocalEditorState

class ProcessingDialog(options: ProcessingOptions, val onClose: (ProcessingOptions) -> Unit) : Dialog() {
    private val optionsState = LocalEditorState(if (options.keepAudioOfPrevious) options.copy(fillAudioOfPrevious = false) else options)

    private var reportedOptions = false

    private lateinit var keepAudioField: Checkbox
    private lateinit var fillAudioField: Checkbox

    private fun currentOptions() = optionsState.current()

    private fun updateOptions(update: (ProcessingOptions) -> ProcessingOptions) = optionsState.update(update)

    init {
        val options = currentOptions()
        isModal = true
        isDraggable = true
        addOpenedChangeListener {
            if (it.isOpened) reportedOptions = false else reportOptions()
        }
        verticalLayout {
            h2("Processing Options")
            span("Without a previous file, donor operations are skipped and new subtitles are retained.")
            checkBox("Keep Video") {
                value = options.keepVideoOfPrevious
                setTooltipText("Keep video of previous option")
                addValueChangeListener { updateOptions { options -> options.copy(keepVideoOfPrevious = it.value) } }
            }
            keepAudioField = checkBox("Keep all previous audio") {
                value = options.keepAudioOfPrevious
                setTooltipText("Append every audio track from the previous file")
                addValueChangeListener {
                    updateOptions { options -> options.copy(keepAudioOfPrevious = it.value, fillAudioOfPrevious = if (it.value) false else options.fillAudioOfPrevious) }
                    if (it.value) fillAudioField.value = false
                }
            }
            fillAudioField = checkBox("Fill missing audio languages") {
                value = options.fillAudioOfPrevious && !options.keepAudioOfPrevious
                setTooltipText("Add missing donor audio languages and their matching signs/forced subtitles")
                addValueChangeListener {
                    updateOptions { options -> options.copy(fillAudioOfPrevious = it.value, keepAudioOfPrevious = if (it.value) false else options.keepAudioOfPrevious) }
                    if (it.value) keepAudioField.value = false
                }
            }
            checkBox("Choose better Audio") {
                value = options.keepBetterAudio
                setTooltipText("Keep one Japanese audio candidate from the selected tracks")
                addValueChangeListener { updateOptions { options -> options.copy(keepBetterAudio = it.value) } }
            }
            integerField("Audio Sync (ms)") {
                value = options.manualAudioSync.toInt()
                isStepButtonsVisible = true
                step = 50
                valueChangeMode = ValueChangeMode.LAZY
                setTooltipText("Delay applied only to donor audio")
                addValueChangeListener { updateOptions { options -> options.copy(manualAudioSync = (it.value ?: 0).toLong()) } }
            }
            integerField("Sub Sync (ms)") {
                value = options.manualSubSync.toInt()
                isStepButtonsVisible = true
                step = 50
                valueChangeMode = ValueChangeMode.LAZY
                setTooltipText("Delay applied only to donor subtitles")
                addValueChangeListener { updateOptions { options -> options.copy(manualSubSync = (it.value ?: 0).toLong()) } }
            }
            checkBox("Discard new subs") {
                value = options.removeNewSubs
                addValueChangeListener { updateOptions { options -> options.copy(removeNewSubs = it.value) } }
            }
            checkBox("Keep all previous subs") {
                value = options.keepSubsOfPrevious
                addValueChangeListener { updateOptions { options -> options.copy(keepSubsOfPrevious = it.value) } }
            }
            checkBox("Keep missing-language subs") {
                value = options.keepSubsMissingLanguages
                addValueChangeListener { updateOptions { options -> options.copy(keepSubsMissingLanguages = it.value) } }
            }
            checkBox("Keep non-english subs") {
                value = options.keepNonEnglish
                addValueChangeListener { updateOptions { options -> options.copy(keepNonEnglish = it.value) } }
            }
            checkBox("Restyle subs") {
                value = options.restyleSubs
                addValueChangeListener { updateOptions { options -> options.copy(restyleSubs = it.value) } }
            }
            checkBox("Remove unnecessary subs/audio") {
                value = options.removeUnnecessary
                addValueChangeListener { updateOptions { options -> options.copy(removeUnnecessary = it.value) } }
            }
            checkBox("Fix tagging") {
                value = options.fixTagging
                addValueChangeListener { updateOptions { options -> options.copy(fixTagging = it.value) } }
            }
            checkBox("Normalize track names") {
                value = options.normalizeTrackNames
                addValueChangeListener { updateOptions { options -> options.copy(normalizeTrackNames = it.value) } }
            }
            details("Other settings") {
                checkBox("Apply TPP to subs") {
                    value = options.tppSubs
                    setTooltipText("Currently has no effect: TPP is not implemented in muxtools-styx.")
                    addValueChangeListener { updateOptions { options -> options.copy(tppSubs = it.value) } }
                }
                span("TPP currently has no effect.")
                textField("Restyle languages") {
                    value = options.restyleLanguages
                    setTooltipText("Comma-separated languages whose ASS subtitles will be restyled")
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { updateOptions { options -> options.copy(restyleLanguages = it.value) } }
                }
                textField("Sub languages") {
                    setTooltipText("Subtitle languages retained when removal is enabled.")
                    value = options.subLanguages
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { updateOptions { options -> options.copy(subLanguages = it.value) } }
                }
                textField("Audio languages") {
                    setTooltipText("Audio languages that will be kept if removal is enabled.")
                    value = options.audioLanguages
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { updateOptions { options -> options.copy(audioLanguages = it.value) } }
                }
            }
        }
    }

    private fun reportOptions() {
        if (!reportedOptions) {
            reportedOptions = true
            onClose(currentOptions())
        }
    }

    override fun onDetach(detachEvent: DetachEvent?) {
        reportOptions()
    }
}
