package moe.styx.web.components.user

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.tooltip
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.theme.lumo.LumoUtility.BoxShadow
import kotlinx.datetime.Clock
import moe.styx.common.data.User
import moe.styx.common.data.toDevice
import moe.styx.db.getUnregisteredDevices
import moe.styx.db.save
import moe.styx.web.Main
import moe.styx.web.getDBClient
import moe.styx.web.topNotification

class AddDeviceDialog(val user: User, doRefresh: () -> Unit = {}) : Dialog() {

    init {
        setWidthFull()
        maxWidth = "550px"
        verticalLayout {
            setWidthFull()
            h3("Device Registration")
            image("${Main.config.imageURL}/website/loginview.webp") {
                tooltip = "Will look something like this"
                addClassNames(BoxShadow.MEDIUM)
            }
            val field = integerField("Code") {
                min = 100000
                max = 999999
                step = 1
                isAutoselect = true
                isAutofocus = true
                isRequiredIndicatorVisible = true
                setWidthFull()
            }
            val nameField = textField("Name") {
                setWidthFull()
                helperText = "Whatever you want to call this device"
                minLength = 1
                maxLength = 30
                isRequiredIndicatorVisible = true
            }

            button("Save") {
                addClickShortcut(Key.ENTER)
                addClickShortcut(Key.NUMPAD_ENTER)
                onLeftClick {
                    if (field.value == null || field.value !in 100000..999999) {
                        topNotification("Please enter a valid code.")
                        return@onLeftClick
                    }
                    if (nameField.value.isNullOrBlank() || nameField.value.length !in 1..30) {
                        topNotification("Please enter a valid name.")
                        return@onLeftClick
                    }
                    getDBClient().execute {
                        val unregistered = getUnregisteredDevices().find { it.code == field.value }
                        if (unregistered == null) {
                            topNotification("Could not find a device for this code.")
                            closeConnection()
                            return@onLeftClick
                        }
                        if (unregistered.codeExpiry < Clock.System.now().epochSeconds) {
                            topNotification("This code is expired.")
                            closeConnection()
                            return@onLeftClick
                        }
                        val device = unregistered.toDevice(user.GUID, nameField.value)
                        if (!save(device)) {
                            topNotification("Could not save device!")
                        } else {
                            topNotification("Added device!")
                            close()
                            closeConnection()
                            doRefresh()
                        }
                    }
                }
            }
        }
    }
}