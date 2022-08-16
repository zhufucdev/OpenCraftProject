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

class FriendshipImpl {
    val a: ServerPlayer
    val b: ServerPlayer
    val id: UUID
    var sharedInventory: Inventory? = null
    private val _checkpoints = mutableSetOf<Checkpoint>()
    val sharedCheckpoints get() = _checkpoints.toList()
    private val extra: Document
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

    constructor(a: ServerPlayer, b: ServerPlayer) {
        this.id = UUID.randomUUID()
        this.doc =
            Document()
                .append("_id", id)
                .append("a", a.uuid)
                .append("b", b.uuid)
        this.extra = Document()
        this.a = a
        this.b = b

        doc["extra"] = extra
        cache[id] = this
        update()
    }

    private constructor(doc: Document) {
        this.a = ServerPlayer.of(uuid = doc.get("a", UUID::class.java))
        this.b = ServerPlayer.of(uuid = doc.get("b", UUID::class.java))
        this.id = doc.get("_id", UUID::class.java)
        this.doc = doc
        this.extra = doc.get("extra", Document::class.java)
            ?: Document().also { this.doc["extra"] = it }

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
        if (sharedInventory == null) {
            sharedInventory = Bukkit.createInventory(null, 45)
        }
    }

    fun addSharedCheckpoint(checkpoint: Checkpoint) {
        _checkpoints.add(checkpoint)
        serializeCheckpoints()
        update()
    }

    fun removeSharedCheckpoint(checkpoint: Checkpoint) {
        _checkpoints.remove(checkpoint)
        serializeCheckpoints()
        update()
    }

    private fun serializeCheckpoints() {
        val chkDoc = Document()
        _checkpoints.forEach {
            chkDoc.append(it.name, Document(it.location.serialize()))
        }
        extra["sharedCheckpoints"] = chkDoc
    }

    fun serializeInventory() {
        val inv = sharedInventory ?: return
        val invDoc = Document()
        inv.forEachIndexed { index, itemStack ->
            if (itemStack != null)
                invDoc[index.toString()] = itemStack.serializeAsBytes()
        }
        extra["sharedInventory"] = invDoc
    }

    var exists = true
        private set

    fun update() {
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
        val cached = cache.values
        fun deserialize(document: Document): FriendshipImpl {
            val a = ServerPlayer.of(uuid = document.get("a", UUID::class.java))
            val b = ServerPlayer.of(uuid = document.get("b", UUID::class.java))
            return between(a, b) ?: FriendshipImpl(document)
        }

        fun of(uuid: UUID) = cache[uuid] ?: Database.friendship(uuid)?.let { deserialize(it) }

        private fun between(a: UUID, b: UUID): FriendshipImpl? {
            val filter: (FriendshipImpl) -> Boolean = { (it.a.uuid == a && it.b.uuid == b) || (it.b.uuid == a && it.a.uuid == b) }
            val index = cache.values.firstOrNull(filter)
            if (index != null)
                return index
            return Database.friendship(a, b)
                ?.let { FriendshipImpl(it) }
        }

        fun between(a: ServerPlayer, b: ServerPlayer): FriendshipImpl? = between(a.uuid, b.uuid)
    }
}