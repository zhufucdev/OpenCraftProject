package com.zhufu.opencraft.player_community

import com.zhufu.opencraft.Language
import com.zhufu.opencraft.ServerPlayer
import com.zhufu.opencraft.contentSize
import com.zhufu.opencraft.info
import org.bukkit.command.CommandSender
import org.bukkit.inventory.Inventory
import java.text.SimpleDateFormat
import java.util.*
import javax.security.auth.Destroyable

class FriendWrap(private val implement: Friend, val parent: ServerPlayer) : Destroyable {
    val isParentAdder = implement.a == parent
    val friend = if (implement.a == parent) implement.b else implement.a
    val name get() = friend.name
    val id get() = implement.id
    var startAt: Long
        get() = implement.startAt
        set(value) {
            implement.startAt = value
        }
    val isFriend get() = implement.isFriend
    var transferred: Long
        get() = implement.transferred
        set(value) {
            implement.transferred = value
        }
    var shareLocation: Boolean
        get() = implement.shareLocation
        set(value) {
            implement.shareLocation = value
        }
    val sharedInventory: Inventory? get() = implement.sharedInventory
    val sharedCheckpoints get() = implement.sharedCheckpoints
    fun createSharedInventory() = implement.createSharedInventory()
    fun statics(getter: Language.LangGetter): List<String> {
        val header = "user.friend.info"
        return listOf(
            getter["$header.time", SimpleDateFormat("yyyy/dd/MM HH:mm:ss").format(Date(startAt))],
            getter["$header.transfer", transferred],
            if (sharedInventory == null)
                getter["$header.noInventory"]
            else
                getter["$header.inventory", sharedInventory!!.contentSize()],
            getter["$header.checkpoints", sharedCheckpoints.size]
        )
    }
    fun printStatics(receiver: CommandSender, getter: Language.LangGetter) {
        receiver.info(getter["user.friend.info.title", this@FriendWrap.name])
        statics(getter).forEach {
            receiver.info(it)
        }
    }
    fun save() = implement.save()
    val exits get() = implement.exists
    fun delete() = implement.delete()

    override fun equals(other: Any?): Boolean =
        other is FriendWrap && other.implement == implement && other.parent == parent

    override fun hashCode(): Int {
        var result = implement.hashCode()
        result = 31 * result + parent.hashCode()
        result = 31 * result + friend.hashCode()
        return result
    }

    private var isDestroyed = false
    override fun isDestroyed(): Boolean = isDestroyed
    override fun destroy() {
        isDestroyed = true
    }
}