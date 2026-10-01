package moe.styx.web.components.downloadable

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.textfield.TextField
import moe.styx.common.data.ProcessingOptions

class ProcessingDialogTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("audio policies are exclusive and new options persist") {
        var saved: ProcessingOptions? = null
        val dialog = ProcessingDialog(ProcessingOptions()) { saved = it }
        dialog.open()
        val components = dialog.processingComponents()
        val checkboxes = components.filterIsInstance<Checkbox>().associateBy { it.label }
        check(checkboxes.values.all { it.isEnabled })
        val keep = checkboxes.getValue("Keep all previous audio")
        val fill = checkboxes.getValue("Fill missing audio languages")
        keep.value = true
        fill.value = true
        check(!keep.value && fill.value)
        keep.value = true
        check(keep.value && !fill.value)
        fill.value = true
        checkboxes.getValue("Normalize track names").value = true
        checkboxes.getValue("Apply TPP to subs").value = true
        components.filterIsInstance<TextField>().single { it.label == "Restyle languages" }.value = "en,fr"
        dialog.close()
        MockVaadin.clientRoundtrip()
        val result = requireNotNull(saved)
        check(result.fillAudioOfPrevious)
        check(!result.keepAudioOfPrevious)
        check(result.normalizeTrackNames)
        check(result.tppSubs)
        check(result.restyleLanguages == "en,fr")
    }

    test("legacy fields survive editing without unsupported controls") {
        var saved: ProcessingOptions? = null
        val dialog = ProcessingDialog(ProcessingOptions(sushiSubs = true, tppStyles = "legacy")) { saved = it }
        dialog.open()
        val components = dialog.processingComponents()
        check(components.filterIsInstance<Checkbox>().none { it.label.contains("sushi", true) })
        check(components.filterIsInstance<TextField>().none { it.label == "TPP Styles" })
        check(components.any { it.element.text == "TPP currently has no effect." })
        dialog.close()
        MockVaadin.clientRoundtrip()
        val result = requireNotNull(saved)
        check(result.sushiSubs && result.tppStyles == "legacy")
    }
})

private fun Component.processingComponents(): List<Component> =
    listOf(this) + children.toList().flatMap { it.processingComponents() }
