package moe.styx.web.components.user

import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.karibudsl.v10.onLeftClick
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.confirmdialog.ConfirmDialog
import com.vaadin.flow.data.renderer.LitRenderer
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer
import com.vaadin.flow.data.renderer.NativeButtonRenderer
import moe.styx.common.data.ActiveUser
import moe.styx.common.data.MediaActivity
import moe.styx.common.data.User
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.common.extension.eqI
import moe.styx.db.*
import moe.styx.web.createComponent
import moe.styx.web.getDBClient
import java.time.Instant
import java.time.ZoneId

fun userListing(dbClient: StyxDBClient) = createComponent {
    verticalLayout {
        button("Add new") {
            onLeftClick {
                UserEditDialog(null).open()
            }
        }
        setWidthFull()
        val online = dbClient.getActiveUsers()
        val users = dbClient.getUsers().associateWith { user -> online.find { user.GUID eqI it.user.GUID } }
        val mapped = UserOnlineCombo.fromMap(users).sortedBy { it.name }
        grid<UserOnlineCombo> {
            setItems(mapped)
            minWidth = "700px"
            setWidthFull()
            addItemClickListener {
                UserEditDialog(it.item.user).open()
            }
            addColumn(
                LitRenderer.of<UserOnlineCombo?>("<a href=\"\${item.link}\" target =\"_blank\">\${item.name}</a>")
                    .withProperty("name", UserOnlineCombo::name)
                    .withProperty("link", UserOnlineCombo::discordURL)
            ).setHeader("Name").setSortable(true)
            addColumn(LocalDateTimeRenderer(UserOnlineCombo::addedDate, "yyyy-MM-dd HH:mm")).setHeader("Added").setSortable(true)
            addColumn(LocalDateTimeRenderer(UserOnlineCombo::lastUsedDate, "yyyy-MM-dd HH:mm")).setHeader("Last Login").setSortable(true)
            addColumn(NativeButtonRenderer({ item -> if (item.user.permissions >= 0) "Disable" else "Enable" }) {
                getDBClient().executeAndClose {
                    save(it.user.copy(permissions = if (it.user.permissions >= 0) -1 else 0))
                    UI.getCurrent().page.reload()
                }
            }).setSortable(false).setWidth("60px").setAutoWidth(false)
            addColumn(NativeButtonRenderer("Delete") { user ->
                ConfirmDialog().apply {
                    setHeader("Do you really want to delete this user?")
                    setConfirmText("Yes")
                    setCancelText("No")
                    setRejectable(false)
                    setCancelable(true)
                    isCloseOnEsc = false
                    addConfirmListener {
                        getDBClient().executeAndClose {
                            delete(user.user)
                            getDevices(mapOf("userID" to user.GUID)).forEach { delete(it) }
                        }

                        UI.getCurrent().page.reload()
                    }
                }.open()
            }).setSortable(false).setWidth("60px").setAutoWidth(false)
        }
    }
}

private data class UserOnlineCombo(
    val name: String,
    val GUID: String,
    val added: Long,
    val discordID: String,
    val lastLogin: Long,
    val isOnline: Boolean,
    val mediaActivity: MediaActivity?,
    val user: User,
) {

    val lastUsedDate = Instant.ofEpochSecond(lastLogin).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val addedDate = Instant.ofEpochSecond(added).atZone(ZoneId.systemDefault()).toLocalDateTime()

    val discordURL: String
        get() = "https://discord.com/users/$discordID"

    companion object {
        fun fromMap(map: Map<User, ActiveUser?>): List<UserOnlineCombo> {
            val list = mutableListOf<UserOnlineCombo>()
            val now = currentUnixSeconds()
            map.forEach { (user, active) ->
                list.add(
                    UserOnlineCombo(
                        user.name,
                        user.GUID,
                        user.added,
                        user.discordID,
                        user.lastLogin,
                        (active?.lastPing ?: 0) > (now - 30),
                        active?.mediaActivity,
                        user
                    )
                )
            }
            return list.toList()
        }
    }
}