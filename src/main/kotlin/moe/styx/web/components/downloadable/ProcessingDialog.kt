package moe.styx.web.components.downloadable

import com.github.mvysny.karibudsl.v10.h2
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.dialog.Dialog
import moe.styx.types.ProcessingOptions

class ProcessingDialog(private var options: ProcessingOptions, val onClose: (ProcessingOptions) -> Unit) : Dialog() {

    init {
        isModal = true
        isDraggable = true
        verticalLayout {
            h2("Processing Options")
        }
    }

    override fun onDetach(detachEvent: DetachEvent?) {
        onClose(options)
    }
}