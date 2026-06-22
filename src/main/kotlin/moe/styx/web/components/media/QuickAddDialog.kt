package moe.styx.web.components.media

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.data.value.ValueChangeMode
import kotlinx.coroutines.runBlocking
import moe.styx.common.data.Media
import moe.styx.web.anilistClient
import moe.styx.web.topNotification
import moe.styx.web.util.createMediaFromAnilist
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
                        val newMedia = createMediaFromAnilist(meta, existingMedia = media)
                        onChoose(newMedia.first())
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
