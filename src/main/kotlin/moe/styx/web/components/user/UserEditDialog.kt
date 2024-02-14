package moe.styx.web.components.user

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import moe.styx.common.data.User
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.db.save
import moe.styx.web.getDBClient
import moe.styx.web.newGUID
import moe.styx.web.topNotification

class UserEditDialog(private val userIn: User?) : Dialog() {

    lateinit var permissionsField: IntegerField
    lateinit var discordIDField: TextField
    lateinit var nameField: TextField

    init {
        setWidthFull()
        maxWidth = "400px"
        verticalLayout(false) {
            h3(if (userIn == null) "Create new user" else "Editing user")
            setWidthFull()
            verticalLayout {
                setWidthFull()

                nameField = textField("Name") {
                    value = userIn?.name ?: ""
                    setWidthFull()
                }
                permissionsField = integerField("Permissions") {
                    setTooltipText("")
                    min = -1
                    value = userIn?.permissions ?: 0
                    max = 100
                    setWidthFull()
                }
                discordIDField = textField("Discord ID") {
                    value = userIn?.discordID ?: ""
                    setWidthFull()
                }
                if (userIn != null) {
                    details("Devices") {
                        deviceListView(userIn)
                    }
                }
            }
            button("Save") {
                onLeftClick {
                    if (nameField.value.isNullOrBlank() || discordIDField.value.isNullOrBlank()) {
                        topNotification("Name and Discord ID are required.")
                        return@onLeftClick
                    }
                    val user = userIn?.copy(permissions = permissionsField.value, name = nameField.value, discordID = discordIDField.value)
                        ?: User(newGUID(), nameField.value, discordIDField.value, currentUnixSeconds(), 0, permissionsField.value)
                    if (!getDBClient().executeGet { save(user) }) {
                        topNotification("Failed to save user!")
                    }
                    close()
                }
            }
        }
    }
}