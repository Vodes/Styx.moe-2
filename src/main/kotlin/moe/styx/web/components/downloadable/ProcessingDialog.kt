package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import moe.styx.common.data.ProcessingOptions

class ProcessingDialog(private var options: ProcessingOptions, private val priority: Int, val onClose: (ProcessingOptions) -> Unit) : Dialog() {

    lateinit var tppstylesField: TextField

    init {
        isModal = true
        isDraggable = true
        verticalLayout {
            h2("Processing Options")
            checkBox("Keep Video") {
                value = options.keepVideoOfPrevious
                isEnabled = priority > 0
                setTooltipText("Keep video of previous option")
                addValueChangeListener { options = options.copy(keepVideoOfPrevious = it.value) }
            }
            checkBox("Keep Audio") {
                value = options.keepAudioOfPrevious
                isEnabled = priority > 0
                setTooltipText("Keep audio of previous option")
                addValueChangeListener { options = options.copy(keepAudioOfPrevious = it.value) }
            }
            checkBox("Choose better Audio") {
                value = options.keepBetterAudio
                setTooltipText("Determine better audio and automatically choose that")
                addValueChangeListener { options = options.copy(keepBetterAudio = it.value) }
            }
            integerField("Audio Sync (ms)") {
                value = options.manualAudioSync.toInt()
                isStepButtonsVisible = true
                step = 50
                valueChangeMode = ValueChangeMode.LAZY
                setTooltipText("Delay to apply to audio that will be muxed in")
                addValueChangeListener { options = options.copy(manualAudioSync = it.value.toLong()) }
            }
            integerField("Sub Sync (ms)") {
                value = options.manualSubSync.toInt()
                isStepButtonsVisible = true
                step = 50
                valueChangeMode = ValueChangeMode.LAZY
                setTooltipText("Delay to apply to subs that will be muxed in")
                addValueChangeListener { options = options.copy(manualSubSync = it.value.toLong()) }
            }
            checkBox("Discard new subs") {
                value = options.removeNewSubs
                isEnabled = priority > 0
                addValueChangeListener { options = options.copy(removeNewSubs = it.value) }
            }
            checkBox("Keep all previous subs") {
                value = options.keepSubsOfPrevious
                isEnabled = priority > 0
                addValueChangeListener { options = options.copy(keepSubsOfPrevious = it.value) }
            }
            checkBox("Keep non-english subs") {
                value = options.keepNonEnglish
                isEnabled = priority > 0
                addValueChangeListener { options = options.copy(keepNonEnglish = it.value) }
            }
            checkBox("Sync subs via sushi") {
                value = options.sushiSubs
                addValueChangeListener { options = options.copy(sushiSubs = it.value) }
            }
            checkBox("Restyle subs") {
                value = options.restyleSubs
                addValueChangeListener { options = options.copy(restyleSubs = it.value) }
            }
            checkBox("Remove unnnecessary subs/audio") {
                value = options.removeUnnecessary
                addValueChangeListener { options = options.copy(removeUnnecessary = it.value) }
            }
            checkBox("Fix tagging") {
                value = options.fixTagging
                addValueChangeListener { options = options.copy(fixTagging = it.value) }
            }
            details("Other settings") {
                checkBox("Apply TPP to subs") {
                    value = options.tppSubs
                    addValueChangeListener {
                        options = options.copy(tppSubs = it.value)
                        tppstylesField.isEnabled = it.value
                    }
                }
                tppstylesField = textField("TPP Styles") {
                    setTooltipText("Styles to apply the tpp to")
                    isEnabled = options.tppSubs
                    value = options.tppStyles
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { options = options.copy(tppStyles = it.value) }
                }
                textField("Sub languages") {
                    setTooltipText("Subtitles that will be processed and/or kept if removal is enabled.")
                    value = options.subLanguages
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { options = options.copy(subLanguages = it.value) }
                }
                textField("Audio languages") {
                    setTooltipText("Audio languages that will be kept if removal is enabled.")
                    value = options.audioLanguages
                    valueChangeMode = ValueChangeMode.LAZY
                    addValueChangeListener { options = options.copy(audioLanguages = it.value) }
                }
            }
        }
    }

    override fun onDetach(detachEvent: DetachEvent?) {
        onClose(options)
    }
}