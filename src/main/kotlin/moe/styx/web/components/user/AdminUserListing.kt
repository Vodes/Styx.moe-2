package moe.styx.web.components.user

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.selectionMode
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.confirmdialog.ConfirmDialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.data.provider.ListDataProvider
import com.vaadin.flow.data.renderer.LitRenderer
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer
import com.vaadin.flow.data.renderer.NativeButtonRenderer
import moe.styx.common.data.ActiveUser
import moe.styx.common.data.MediaActivity
import moe.styx.common.data.User
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.common.extension.eqI
import moe.styx.db.tables.ActiveUserTable
import moe.styx.db.tables.UserTable
import moe.styx.web.createComponent
import moe.styx.web.dbClient
import moe.styx.web.topNotification
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.time.ZoneId

fun userListing(readonly: Boolean = false) = createComponent {
    verticalLayout {
        if (readonly)
            nativeLabel("Due to lacking permissions, this view is in readonly mode.")
        button("Add new") {
            isEnabled = !readonly
            onClick {
                UserEditDialog(null).open()
            }
        }
        setWidthFull()
        val online = dbClient.transaction { ActiveUserTable.query { selectAll().toList() } }
        val users =
            dbClient.transaction { UserTable.query { selectAll().toList() } }.associateWith { user -> online.find { user.GUID eqI it.user.GUID } }
        val mapped = ListDataProvider(UserOnlineCombo.fromMap(users).sortedBy { it.name })
        grid<UserOnlineCombo> {
            setItems(mapped)
            setWidthFull()
            selectionMode = Grid.SelectionMode.NONE
            setWidthFull()
            addItemClickListener {
                UserEditDialog(it.item.user, readonly).open()
            }
            addColumn(
                LitRenderer.of<UserOnlineCombo?>("<a href=\"\${item.link}\" target =\"_blank\">\${item.name}</a>")
                    .withProperty("name", UserOnlineCombo::name)
                    .withProperty("link", UserOnlineCombo::discordURL)
            ).setHeader("Name").setFlexGrow(1).setSortable(true)
            addColumn(LocalDateTimeRenderer(UserOnlineCombo::addedDate, "yyyy-MM-dd HH:mm")).setHeader("Added").setFlexGrow(1).setAutoWidth(true)
                .setSortable(true)
            addColumn(LocalDateTimeRenderer(UserOnlineCombo::lastUsedDate, "yyyy-MM-dd HH:mm")).setHeader("Last Login").setFlexGrow(1)
                .setAutoWidth(true)
                .setSortable(true)
            addColumn(NativeButtonRenderer({ item -> if (item.user.permissions >= 0) "Disable" else "Enable" }) {
                if (readonly) {
                    topNotification("You don't have the permissions to do this.")
                    return@NativeButtonRenderer
                }
                dbClient.transaction {
                    UserTable.upsertItem(it.user.copy(permissions = if (it.user.permissions >= 0) -1 else 0))
                }
                UI.getCurrent().page.reload()
            }).setSortable(false).setWidth("60px").setAutoWidth(false)
            addColumn(NativeButtonRenderer("Delete") { user ->
                if (readonly) {
                    topNotification("You don't have the permissions to do this.")
                    return@NativeButtonRenderer
                }
                ConfirmDialog().apply {
                    setHeader("Do you really want to delete this user?")
                    setConfirmText("Yes")
                    setCancelText("No")
                    setRejectable(false)
                    setCancelable(true)
                    isCloseOnEsc = false
                    addConfirmListener {
                        dbClient.transaction {
                            UserTable.deleteWhere { GUID eq user.GUID }
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