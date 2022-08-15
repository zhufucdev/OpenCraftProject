package com.zhufu.opencraft.player_community

import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.contentSize
import com.zhufu.opencraft.data.Checkpoint
import com.zhufu.opencraft.data.ServerPlayer
import com.zhufu.opencraft.info
import org.bukkit.command.CommandSender
import org.bukkit.inventory.Inventory
import java.text.SimpleDateFormat
import java.util.*
import javax.security.auth.Destroyable

class Friendship(private val impl: FriendshipImpl, val owner: ServerPlayer) : Destroyable {
    val isParentAdder = impl.a == owner
    val friend = if (impl.a == owner) impl.b else impl.a
    val name get() = friend.name
    val id get() = impl.id
    var startAt: Long
        get() = impl.startAt
        set(value) {
            impl.startAt = value
        }
    val isFriend get() = impl.isFriend
    var transferred: Long
        get() = impl.transferred
        set(value) {
            impl.transferred = value
        }
    var shareLocation: Boolean
        get() = impl.shareLocation
        set(value) {
            impl.shareLocation = value
        }
    val sharedInventory: Inventory? get() = impl.sharedInventory
    val sharedCheckpoints get() = impl.sharedCheckpoints
    fun createSharedInventory() = impl.createSharedInventory()
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
        receiver.info(getter["user.friend.info.title", this@Friendship.name])
        statics(getter).forEach {
            receiver.info(it)
        }
    }

    fun shareCheckpoint(checkpoint: Checkpoint) = impl.addSharedCheckpoint(checkpoint)
    fun removeSharedCheckpoint(checkpoint: Checkpoint) = impl.removeSharedCheckpoint(checkpoint)

    val exits get() = impl.exists
    fun delete() = impl.delete()

    override fun equals(other: Any?): Boolean =
        other is Friendship && other.impl == impl

    override fun hashCode(): Int {
        var result = impl.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + friend.hashCode()
        return result
    }

    private var isDestroyed = false
    override fun isDestroyed(): Boolean = isDestroyed
    override fun destroy() {
        isDestroyed = true
    }
}