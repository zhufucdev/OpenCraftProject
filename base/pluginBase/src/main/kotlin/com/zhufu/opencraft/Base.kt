package com.zhufu.opencraft

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.player_community.PublicMessagePool
import org.bukkit.*
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import java.io.File
import java.nio.file.Paths
import java.util.TimeZone
import java.util.UUID
import java.util.function.Consumer
import kotlin.concurrent.fixedRateTimer
import kotlin.random.Random

object Base {
    /* Data Save */
    lateinit var spawnWorld: World
    lateinit var surviveWorld: World
    lateinit var netherWorld: World
    lateinit var endWorld: World
    lateinit var lobby: World
    lateinit var tradeWorld: World

    val timeZone = TimeZone.getTimeZone("Asia/Shanghai")

    /* Extended Functions */
    fun getRandomLocation(world: World, bound: Int, x: Int? = null, y: Int? = null, z: Int? = null): Location =
        Location(world, x?.toDouble() ?: random(bound), y?.toDouble() ?: random(bound), z?.toDouble() ?: random(bound))

    fun getRandomLocation(location: Location, x: Int? = null, y: Int? = null, z: Int? = null, bound: Int) =
        location.add(x?.toDouble() ?: random(bound), y?.toDouble() ?: random(bound), z?.toDouble() ?: random(bound))

    val random = Random.Default
    fun random(bound: Int): Double =
        (if (trueByPercentages(0.5f)) 1 else -1) * random.nextDouble() * random.nextInt(bound + 1)

    fun trueByPercentages(p: Float): Boolean {
        if (p <= 0) return false
        if (p >= 1) return true
        var n = 1
        while ((p * 10 * n).toInt() - p * 10 * n != 0.toFloat()) {
            n++
        }
        val r = random.nextInt(10 * n) - 1
        return r in 0 until (p * 10 * n).toInt()
    }

    fun getUniquePair(order: Int): Pair<Int, Int> {
        var first = true
        var turning = 1
        var x = 0
        var z = 0
        var dir = 0

        /**
         * 0 => UP
         * 1 => LEFT
         * 2 => DOWN
         * 3 => RIGHT
         */
        var direction = 0
        for (i in 0 until order) {
            when (direction) {
                0 -> z++
                1 -> x--
                2 -> z--
                3 -> x++
            }
            dir++
            if (dir >= turning) {
                direction++
                if (direction > 3)
                    direction = 0
                first = if (!first) {
                    turning++
                    true
                } else
                    false
                dir = 0
            }
        }
        return x to z
    }

    @Deprecated("Outdated", ReplaceWith("PublicMessagePool"))
    val publicMsgPool get() = PublicMessagePool
    val serverID get() = UUID.fromString("33B67ABC-F831-4FA5-A37E-BE289B8D5C98")

    object Extend {
        fun String.isDigit(): Boolean {
            var a = 0
            var b = 0
            this.forEach {
                if (it == '-')
                    a++
                else if (it == '.')
                    b++
                else if (!it.isDigit()) {
                    return false
                }
                if (a > 1 || b > 1)
                    return false
            }
            return true
        }

        fun Location.toPrettyString(): String = "${world!!.name}($x,$y,$z)"
        fun Location.appendToJson(writer: JsonWriter) {
            writer
                .beginObject()
                .name("world").value(world!!.uid.toString())
                .name("x").value(x)
                .name("y").value(y)
                .name("z").value(z)
                .name("pitch").value(pitch)
                .name("yaw").value(yaw)
                .endObject()
        }

        fun fromJsonToLocation(reader: JsonReader): Location? {
            var world: World? = null
            var x = Double.NaN
            var y = Double.NaN
            var z = Double.NaN
            var pitch = Float.NaN
            var yaw = Float.NaN

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "world" -> world = Bukkit.getWorld(UUID.fromString(reader.nextString()))
                    "x" -> x = reader.nextDouble()
                    "y" -> y = reader.nextDouble()
                    "z" -> z = reader.nextDouble()
                    "yaw" -> yaw = reader.nextDouble().toFloat()
                    "pitch" -> pitch = reader.nextDouble().toFloat()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            if (world == null || x.isNaN() || y.isNaN() || z.isNaN() || pitch.isNaN() || yaw.isNaN()) {
                return null
            }
            return Location(world, x, y, z, yaw, pitch)
        }
    }

    val pluginCore: Plugin = Bukkit.getPluginManager().getPlugin("ServerCore")!!

    object TutorialUtil {
        fun Entity.tplock(location: Location, time: Long) {
            var i = 0L
            Bukkit.getScheduler().runTaskTimer(pluginCore, Consumer {
                teleport(location)
                i++
                if (i >= time) {
                    it.cancel()
                }
            }, 0, 1)
        }

        fun Player.gmd(mode: GameMode) {
            Bukkit.getScheduler().runTask(pluginCore) { _ ->
                gameMode = mode
            }
        }

        fun Entity.linearMotion(
            location: Location,
            ticks: Long,
            done: (() -> Unit)? = null,
            ignoreYaw: Boolean = false
        ) {
            val old = this.location.clone()
            val locationOnce =
                Vector((location.x - old.x) / ticks, (location.y - old.y) / ticks, (location.z - old.z) / ticks)
            val pitchOnce = (location.pitch - old.pitch) / ticks
            var yawOnce = if (!ignoreYaw) location.yaw - old.yaw else 0f
            if (!ignoreYaw) {
                if (yawOnce > 180) {
                    yawOnce = 360 - yawOnce
                } else if (yawOnce < -180) {
                    yawOnce += 360
                }
                yawOnce = yawOnce / ticks * 2.557.toFloat()
            }

            var i = 0L
            Bukkit.getScheduler().runTaskTimer(pluginCore, Consumer {
                teleport(
                    this.location.clone().add(locationOnce).apply {
                        pitch += pitchOnce
                        if (!ignoreYaw)
                            yaw += yawOnce
                    }
                )
                i++
                if (i >= ticks) {
                    it.cancel()
                    teleport(location)
                    done?.invoke()
                }
            }, 0, 1)
        }
    }
}