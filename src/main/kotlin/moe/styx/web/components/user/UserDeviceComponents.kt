package moe.styx.web.components.user

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.common.data.User
import moe.styx.db.delete
import moe.styx.db.getDevices
import moe.styx.web.getDBClient
import moe.styx.web.replaceAll
import moe.styx.web.toISODate
import moe.styx.web.topNotification
import org.vaadin.lineawesome.LineAwesomeIcon

class DeviceListView(private val user: User, private val readonly: Boolean = false) : KComposite() {
    private lateinit var deviceLayout: VerticalLayout


    val root = ui {
        verticalLayout {
            horizontalLayout(false) {
                h3("Devices")
                iconButton(LineAwesomeIcon.PLUS_SOLID.create()) {
                    isEnabled = !readonly
                    onLeftClick {
                        AddDeviceDialog(user) { updateList() }.open()
                    }
                }
            }
            deviceLayout = verticalLayout {}
            updateList()
        }
    }

    private fun updateList() {
        getDBClient().executeAndClose {
            deviceLayout.replaceAll {
                val devices = getDevices(mapOf("userID" to user.GUID))
                if (devices.isEmpty()) {
                    h3("No devices found.")
                } else {
                    verticalLayout(false) {
                        devices.forEachIndexed { index, device ->
                            verticalLayout(false) {
                                if (index != 0)
                                    addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_30)
                                horizontalLayout(false) {
                                    defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                                    h4(device.name)
                                    iconButton(LineAwesomeIcon.TRASH_SOLID.create()) {
                                        isEnabled = !readonly
                                        onLeftClick {
                                            val deleted = getDBClient().executeGet { delete(device) }
                                            if (!deleted) {
                                                topNotification("Could not delete device!")
                                                return@onLeftClick
                                            }
                                            topNotification("Deleted device.")
                                            updateList()
                                        }
                                    }
                                }
                                nativeLabel("Last used: ${device.lastUsed.toISODate()}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).deviceListView(
    user: User,
    readonly: Boolean = false,
    block: (@VaadinDsl DeviceListView).() -> Unit = {}
) = init(
    DeviceListView(user, readonly), block
)