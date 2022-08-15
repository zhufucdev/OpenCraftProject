package com.zhufu.opencraft.player_community

import com.mongodb.client.model.Filters
import com.zhufu.opencraft.Base
import com.zhufu.opencraft.data.Database
import com.zhufu.opencraft.data.ServerPlayer
import org.bson.Document
import java.util.*

class PlayerStatics private constructor(val owner: ServerPlayer) {
    val collection = Database.statics(owner.uuid)
    private fun getToday() = Calendar.getInstance().apply {
        time = Date()
        timeZone = Base.timeZone
        set(Calendar.SECOND, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun getYesterday() = Calendar.getInstance().apply {
        time = Date()
        timeZone = Base.timeZone
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
    private var dT: Double = document(today)?.getDouble("damage") ?: 0.0
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

    private var cY: Long = document(getYesterday())?.getLong("currency") ?: owner.currency.also {
        setCurrency(owner.currency)
    }
    val currencyDelta: Long
        get() {
            syncDate {
                cY =
                    document(getYesterday())?.getLong("currency") ?: owner.currency.also { setCurrency(owner.currency) }
            }
            return owner.currency - cY
        }

    fun setCurrency(current: Long) {
        syncDate()
        set(today, "currency", current)
    }

    operator fun get(time: Long) = document(time)?.getLong("time") ?: 0L

    private fun document(day: Long) = try {
        collection.find(Filters.eq(day)).first()
    } catch (e: Exception) {
        null
    }

    private fun documentOrCreate(day: Long) = document(day)
        ?: Document("_id", day).also { collection.insertOne(it) }

    operator fun set(day: Long, value: Long) {
        documentOrCreate(day)["time"] = value
    }

    fun set(day: Long, key: String, value: Number) {
        val doc = documentOrCreate(day)
        doc[key] = value
        collection.replaceOne(Filters.eq(day), doc)
    }

    private fun syncDate(onNewDay: (() -> Unit)? = null) {
        val newTime = getToday()
        if (today != newTime) {
            onNewDay?.invoke()
            today = newTime
        }
    }

    fun getDailyPlayTime() = collection.find().map { it.getLong("_id") to it.getLong("time") }.toList()

    fun copyFrom(other: PlayerStatics) {
        collection.drop()
        Database.statics(owner.uuid).insertMany(other.collection.find().toList())
    }

    fun delete() {
        collection.drop()
    }

    companion object {
        private val map = HashMap<UUID, PlayerStatics>()
        fun forEach(l: (PlayerStatics) -> Unit) {
            map.forEach { (_, u) ->
                l(u)
            }
        }

        fun remove(uuid: UUID) = map.remove(uuid)

        fun contains(player: ServerPlayer) = map.containsKey(player.uuid)
        fun from(player: ServerPlayer): PlayerStatics? {
            val uuid = player.uuid
            if (map.containsKey(uuid)) {
                return map[uuid]!!
            }
            return PlayerStatics(player)
                .also { map[uuid] = it }
        }
    }
}