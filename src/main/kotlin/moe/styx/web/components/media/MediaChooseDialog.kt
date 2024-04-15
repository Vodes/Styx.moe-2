package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Div
import moe.styx.common.data.Media
import moe.styx.web.components.MediaGrid

class MediaChooseDialog(private val exclude: String, val onClose: (Media?) -> Unit) : Dialog() {
    private var selected: Media? = null

    init {
        val layout = Div()
        layout.setSizeFull()
        layout.add(verticalLayout {
            setSizeFull()
            add(MediaGrid(exclude) { selected = it; this@MediaChooseDialog.close() })
        })
        add(layout)
    }

    override fun onDetach(detachEvent: DetachEvent?) {
        onClose(selected)
    }
}