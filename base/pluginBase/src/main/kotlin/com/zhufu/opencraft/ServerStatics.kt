package com.zhufu.opencraft

import com.mongodb.client.model.Filters
import com.zhufu.opencraft.data.Database
import org.bson.Document
import org.bukkit.Bukkit
import java.time.LocalDate
import java.time.LocalDateTime

object ServerStatics {
    private val collection = Database.statics(Base.serverID)

    private fun getToday() = LocalDate.now(Base.timeZone.toZoneId())
        .atStartOfDay()
        .atZone(Base.timeZone.toZoneId())
        .toInstant()
        .epochSecond

    val playerCount: Int
        get() = Database.tag.countDocuments().toInt()
    fun record() {
        val timestamp = LocalDateTime.now().atZone(Base.timeZone.toZoneId()).toInstant().epochSecond
        collection.insertOne(
            Document("_id", timestamp)
                .append("tps", Bukkit.getTPS())
                .append("online_players", Bukkit.getOnlinePlayers().size)
                .append("total_players", playerCount)
                .append("loaded_chunks", Base.surviveWorld.loadedChunks.size)
                .append("total_chunks", Base.surviveWorld.chunkCount)
        )
    }
}