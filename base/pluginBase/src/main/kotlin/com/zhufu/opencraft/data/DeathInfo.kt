package com.zhufu.opencraft.data

import com.zhufu.opencraft.Base.Extend.toPrettyString
import com.zhufu.opencraft.getter
import org.bson.Document
import org.bukkit.Location
import org.bukkit.entity.Player

data class DeathInfo(val time: Long, val reason: String, val location: Location) {
    fun teleport(player: Player) {
        player.teleport(location)
    }

    fun show(player: Player) {
        val getter = player.getter()
        player.sendMessage(
            *arrayOf(
                "${getter["user.lastDeath.time"]}: ${if (time == -1L) "null" else time.toString()}",
                "${getter["user.lastDeath.location"]}: ${location.toPrettyString()}",
                "${getter["user.lastDeath.reason"]}: ${reason.ifEmpty { "null" }}"
            )
        )
    }

    fun toDocument() = Document(mapOf(
        "time" to time,
        "reason" to reason,
        "location" to Document(location.serialize())
    ))

    companion object {
        fun deserialize(doc: Document) = DeathInfo(
            doc.getLong("time"),
            doc.getString("reason"),
            Location.deserialize(doc.get("location", Document::class.java))
        )
    }
}
