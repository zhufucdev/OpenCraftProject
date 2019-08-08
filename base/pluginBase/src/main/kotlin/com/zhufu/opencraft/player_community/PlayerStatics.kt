package com.zhufu.opencraft.player_community

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.zhufu.opencraft.ServerPlayer
import org.bukkit.Bukkit
import java.nio.file.Paths
import java.util.*
import kotlin.collections.HashMap

class PlayerStatics private constructor(val parent: ServerPlayer, private var data: JsonObject) {
    private fun getToday() = Calendar.getInstance().apply {
        time = Date()
        set(Calendar.SECOND, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private var today = getToday()
    private var tT = get(today)
    var timeToday: Long
        get() {
            val newTime = getToday()
            if (today != newTime) {
                today = newTime
                tT = 0
            }
            return tT
        }
        set(value) {
            val newTime = getToday()
            if (today != newTime) {
                set(today, tT)
                today = newTime
            }
            set(today, value)
            tT = value
        }

    operator fun get(time: Long) = data[time.toString()]?.asLong ?: 0L
    operator fun set(time: Long, value: Long) = data.addProperty(time.toString(), value)

    fun getData() = data

    val file by lazy { Paths.get(
        "plugins",
        "statics",
        parent.uuid!!.toString() + ".json"
    ).toFile() }

    fun save() {
        file.apply {
            if (!exists()) {
                if (!parentFile.exists())
                    parentFile.mkdirs()
                createNewFile()
            }
            val writer = this.writer()
            GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(data, writer)
            writer.apply {
                flush()
                close()
            }
        }
    }

    fun copyFrom(other: PlayerStatics) {
        data = other.data
        save()
    }

    fun delete() {
        file.delete()
    }

    companion object {
        private val map = HashMap<UUID, PlayerStatics>()
        fun forEach(l: (PlayerStatics) -> Unit) {
            map.forEach { _, u ->
                l(u)
            }
        }

        fun cleanUp() {
            forEach { it.save() }
            map.clear()
        }

        fun remove(uuid: UUID) = map.remove(uuid)

        fun contains(player: ServerPlayer) = player.uuid?.let { map.containsKey(it) } ?: false
        fun from(player: ServerPlayer): PlayerStatics? {
            val uuid = player.uuid ?: return null
            if (map.containsKey(uuid)) {
                return map[uuid]!!
            }
            val file =
                Paths.get(
                    "plugins",
                    "statics",
                    "$uuid.json"
                ).toFile()
            val data = if (!file.exists()) {
                file.parentFile.apply {
                    if (!exists()) mkdirs()
                }
                file.createNewFile()

                JsonObject()
            } else {
                try {
                    JsonParser().parse(file.reader()).asJsonObject
                } catch (e: Exception) {
                    Bukkit.getLogger()
                        .warning("Failed to load statics file for ${player.name}. Using an empty one instead.")
                    JsonObject()
                }
            }
            return PlayerStatics(player, data)
                .also { map[uuid] = it }
        }
    }
}