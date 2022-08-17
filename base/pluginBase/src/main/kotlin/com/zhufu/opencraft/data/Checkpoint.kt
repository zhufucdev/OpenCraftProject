package com.zhufu.opencraft.data

import com.mongodb.client.model.Filters.eq

import com.zhufu.opencraft.api.Nameable
import org.bson.Document
import org.bukkit.Location
import java.util.*

class Checkpoint(val location: Location, name: String) : Nameable, Cloneable {
    var parent: Checkpoints? = null

    private var _name: String = name
    override var name: String
        get() = _name
        set(value) {
            val oldName = _name
            _name = value
            parent?.update(this, oldName)
        }

    fun toDocument() = Document(
        mapOf("name" to name)
                + location.serialize()
    )

    override fun equals(other: Any?): Boolean {
        return other is Checkpoint
                && other.location == this.location
                && other.name == this.name
    }

    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}

class Checkpoints private constructor(val owner: ServerPlayer) : Iterable<Checkpoint> {
    val doc = Database.checkpoint(owner.uuid)
    fun add(checkpoint: Checkpoint) {
        checkpoint.parent = this
        doc.insertOne(checkpoint.toDocument())
    }

    fun update(record: Checkpoint, oldName: String) {
        doc.replaceOne(eq("name", oldName), record.toDocument())
    }

    fun remove(checkpoint: Checkpoint) {
        doc.deleteOne(eq("name", checkpoint.name))
    }

    fun contains(checkpoint: Checkpoint) = doc.find(eq("name", checkpoint.name)).first() != null

    companion object {
        private val cache = hashMapOf<UUID, Checkpoints>()
        fun of(player: ServerPlayer) = cache[player.uuid] ?: Checkpoints(player).also { cache[player.uuid] = it }
    }

    override fun iterator() =
        doc.find().map { Checkpoint(Location.deserialize(it), it.getString("name")) }.iterator()
}