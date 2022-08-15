package com.zhufu.opencraft.player_community

import com.zhufu.opencraft.data.Checkpoint
import com.zhufu.opencraft.data.Database
import com.zhufu.opencraft.data.ServerPlayer
import org.bson.Document
import org.bson.types.Binary
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.io.File
import java.nio.file.Paths
import java.util.*

class FriendshipImpl(
    val a: ServerPlayer,
    val b: ServerPlayer,
    startAt: Long = -1,
    val id: UUID = UUID.randomUUID(),
    doc: Document? = null,
    private val extra: Document = Document()
) {
    var sharedInventory: Inventory? = null
    private val _checkpoints = mutableSetOf<Checkpoint>()
    val sharedCheckpoints get() = _checkpoints.toList()
    var transferred: Long
        get() = extra.getLong("transfer") ?: 0
        set(value) {
            extra["transfer"] = value
            update()
        }
    var shareLocation: Boolean
        get() = extra.getBoolean("shareLocation", false)
        set(value) {
            extra["shareLocation"] = value
            update()
        }
    val isFriend get() = startAt != -1L

    val doc: Document
    var startAt: Long
        get() = doc.getLong("startAt") ?: -1
        set(value) {
            doc["startAt"] = value
            update()
        }

    init {
        this.doc = doc ?: Document(mapOf(
            "_id" to id,
            "a" to a.uuid,
            "b" to b.uuid,
            "extra" to extra
        ))
        this.startAt = startAt

        if (isFriend) {
            if (extra.containsKey("sharedInventory")) {
                createSharedInventory()
                val invDoc = extra.get("sharedInventory", Document::class.java)
                invDoc.forEach { (t, u) ->
                    sharedInventory!!.setItem(t.toInt(), ItemStack.deserializeBytes((u as Binary).data))
                }
            }
            if (extra.containsKey("sharedCheckpoints")) {
                val config = extra.get("sharedCheckpoints", Document::class.java)
                config.forEach { (t, u) ->
                    _checkpoints.add(
                        Checkpoint(
                            location = Location.deserialize(u as Document),
                            name = t
                        )
                    )
                }
            }
        }
        cache[id] = this
    }

    fun createSharedInventory() {
        if (sharedInventory == null)
            sharedInventory = Bukkit.createInventory(null, 45)
    }

    fun addSharedCheckpoint(checkpoint: Checkpoint) {
        _checkpoints.add(checkpoint)
        update()
    }

    fun removeSharedCheckpoint(checkpoint: Checkpoint) {
        _checkpoints.remove(checkpoint)
        update()
    }

    var exists = true
        private set

    private fun update() {
        Database.friendship(id, doc)
    }

    fun delete() {
        a.friendships.remove(b)
        cache.remove(id)
        exists = false
    }

    override fun equals(other: Any?): Boolean =
        other is FriendshipImpl
                && other.id == id
                && other.startAt == startAt

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + a.hashCode()
        result = 31 * result + b.hashCode()
        result = 31 * result + startAt.hashCode()
        return result
    }

    companion object {
        private val cache = HashMap<UUID, FriendshipImpl>()

        fun deserialize(document: Document): FriendshipImpl {
            val uuid = document.get("_id", UUID::class.java)
            return FriendshipImpl(
                a = ServerPlayer.of(
                    uuid = document.get("a", UUID::class.java)
                ),
                b = ServerPlayer.of(
                    uuid = document.get("b", UUID::class.java)
                ),
                startAt = document.getLong("startAt"),
                id = uuid,
                doc = document,
                extra = document.get("extra", Document::class.java)
            )
        }

        fun of(uuid: UUID) = cache[uuid] ?: Database.friendship(uuid)?.let { deserialize(it) }

        fun between(a: ServerPlayer, b: ServerPlayer): FriendshipImpl? {
            val filter: (FriendshipImpl) -> Boolean = { (it.a == a && it.b == b) || (it.b == a && it.a == b) }
            val index = cache.values.firstOrNull(filter)
            if (index != null)
                return index
            return Database.friendship(a.uuid, b.uuid)
                ?.let { deserialize(it) }
                ?.also { cache[it.id] = it }
        }
    }
}