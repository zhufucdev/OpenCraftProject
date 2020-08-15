package com.zhufu.opencraft.player_community

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.zhufu.opencraft.ServerPlayer
import org.bukkit.Bukkit
import java.io.File
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

    private fun getYesterday() = Calendar.getInstance().apply {
        time = Date()
        set(Calendar.SECOND, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MILLISECOND, 0)
        set(Calendar.DAY_OF_YEAR, get(Calendar.DAY_OF_YEAR) - 1)
    }.timeInMillis

    private var today = getToday()
    private var tT = get(today)
    var timeToday: Long
        get() {
            syncDate { tT = 0 }
            return tT
        }
        set(value) {
            syncDate { tT = 0 }
            set(today, value)
            tT = value
        }
    private var dT: Double = get(today, "damage")?.asDouble ?: 0.0
    var damageToday: Double
        get() {
            syncDate { dT = 0.0 }
            return dT
        }
        set(value) {
            syncDate { dT = 0.0 }
            set(today, "damage", value)
            dT = value
        }

    private var cY: Long = get(getYesterday(), "currency")?.asLong ?: parent.currency.also {
        setCurrency(parent.currency)
    }
    val currencyDelta: Long
        get() {
        syncDate {
            cY = get(getYesterday(), "currency")?.asLong ?: parent.currency.also { setCurrency(parent.currency) }
        }
        return parent.currency - cY
    }

    fun setCurrency(current: Long) {
        syncDate()
        set(timeToday, "currency", current)
    }

    operator fun get(time: Long) = data[time.toString()]?.let {
        if (it.isJsonObject) {
            it.asJsonObject["time"]?.asLong
        } else {
            it.asLong
        }
    } ?: 0L

    fun get(time: Long, key: String) = try { data[time.toString()]?.asJsonObject?.get(key) } catch (e: Exception) { null }

    operator fun set(time: Long, value: Long) {
        val obj = JsonObject()
        obj.addProperty("time", value)
        data.add(time.toString(), obj)
    }

    fun set(time: Long, key: String, value: Number) {
        val obj = data[time.toString()]?.let {
            if (it.isJsonObject) {
                it.asJsonObject
            } else {
                JsonObject().apply {
                    addProperty("time", it.asLong)
                }
            }
        } ?: JsonObject()
        obj.addProperty(key, value)
        data.add(time.toString(), obj)
    }

    private fun syncDate(onNewDay: (() -> Unit)? = null) {
        val newTime = getToday()
        if (today != newTime) {
            onNewDay?.invoke()
            today = newTime
        }
    }

    fun getData() = data

    val file: File by lazy {
        Paths.get(
            "plugins",
            "statics",
            parent.uuid!!.toString() + ".json"
        ).toFile()
    }

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

        fun saveAll() {
            forEach { it.save() }
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