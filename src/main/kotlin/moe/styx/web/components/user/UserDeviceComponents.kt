package moe.styx.web.components.user

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.theme.lumo.LumoUtility
import moe.styx.common.data.User
import moe.styx.db.tables.DeviceTable
import moe.styx.web.dbClient
import moe.styx.web.replaceAll
import moe.styx.web.toISODate
import moe.styx.web.topNotification
import org.jetbrains.exposed.sql.selectAll
import org.vaadin.lineawesome.LineAwesomeIcon

class DeviceListView(private val user: User, private val readonly: Boolean = false) : KComposite() {
    private lateinit var deviceLayout: VerticalLayout


    val root = ui {
        verticalLayout {
            horizontalLayout(false) {
                h3("Devices")
                iconButton(LineAwesomeIcon.PLUS_SOLID.create()) {
                    isEnabled = !readonly
                    onClick {
                        AddDeviceDialog(user) { updateList() }.open()
                    }
                }
            }
            deviceLayout = verticalLayout {}
            updateList()
        }
    }

    private fun updateList() {
        deviceLayout.replaceAll {
            val devices = dbClient.transaction { DeviceTable.query { selectAll().where { userID eq user.GUID }.toList() } }
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
                                    onClick {
                                        val deleted = dbClient.transaction { DeviceTable.delete(device) }
                                        if (!deleted) {
                                            topNotification("Could not delete device!")
                                            return@onClick
                                        }
                                        topNotification("Deleted device.")
                                        updateList()
                                    }
                                }
                            }
                            nativeLabel("Last used: ${device.lastUsed.toISODate()}")
                            details("Hardware Info") {
                                val info = device.deviceInfo
                                verticalLayout(false) {
                                    horizontalLayout(false) {
                                        defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                                        when (device.deviceInfo.type.lowercase()) {
                                            "pc" -> add(LineAwesomeIcon.DESKTOP_SOLID.create())
                                            "laptop" -> add(LineAwesomeIcon.LAPTOP_SOLID.create())
                                            "phone" -> add(LineAwesomeIcon.MOBILE_SOLID.create())
                                            "tablet" -> add(LineAwesomeIcon.TABLET_SOLID.create())
                                        }
                                        nativeLabel(info.os + if (info.osVersion.isNullOrBlank()) "" else " (${info.osVersion})")
                                    }
                                    if (info.type.lowercase() in arrayOf("phone", "tablet"))
                                        htmlSpan("<b>Model:</b> ${if (info.model.isNullOrBlank()) "Unknown" else info.model}")

                                    htmlSpan("<b>CPU:</b> ${if (info.cpu.isNullOrBlank()) "Unknown" else info.cpu}")

                                    if (info.type.lowercase() !in arrayOf("phone", "tablet")) {
                                        htmlSpan("<b>GPU:</b> ${if (info.gpu.isNullOrBlank()) "Unknown" else info.gpu}")
                                        val jvmString = info.jvm?.let {
                                            if (!info.jvmVersion.isNullOrBlank() && info.jvm?.contains(info.jvmVersion!!) == false)
                                                "$it (${info.jvmVersion}"
                                            it
                                        }
                                        htmlSpan("<b>JVM:</b> ${if (info.jvm.isNullOrBlank()) "Unknown" else jvmString}")
                                    }
                                }
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