package com.zhufu.opencraft.headers.npc_wrap

import com.zhufu.opencraft.Info
import com.zhufu.opencraft.Language
import com.zhufu.opencraft.ServerPlayer
import com.zhufu.opencraft.headers.player_wrap.SimpleLocation
import com.zhufu.opencraft.script.AbstractScript
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*
import java.util.function.Function

@Suppress("unused")
class NPCUtils(private val getter: Language.LangGetter, private val script: AbstractScript, private val player: ServerPlayer? = null) {
    private fun <T> validate(name: String, clazz: Class<T>, map: AbstractMap<*, *>) {
        val realName = if (name.contains('/')) name.substringAfterLast('/') else name
        validate(name, map)
        if (!map[realName]!!.javaClass.let {
                it.isAssignableFrom(clazz)
                        || it == clazz
                        || (clazz == Number::class.java && (
                        it == Integer::class.java
                                || it == Long::class.java
                                || it == Double::class.java
                        ))
            }) {
            throw IllegalStateException(getter["npc.error.wrongClass", name])
        }
    }

    private fun validate(name: String, map: AbstractMap<*, *>) {
        val realName = if (name.contains('/')) name.substringAfterLast('/') else name
        if (!map.containsKey(realName))
            throw IllegalArgumentException(getter["npc.error.parNotFound", name])
    }

    fun create(map: Any?): SimpleNPC {
        if (map is AbstractMap<*, *>) {
            validate("name", String::class.java, map)
            val name = map["name"]!! as String
            validate("spawnpoint", map)
            val spawnpointLocation: Location =
                when (val spawnpoint = map["spawnpoint"]) {
                    is AbstractMap<*, *> -> {
                        validate("spawnpoint/x", Number::class.java, spawnpoint)
                        validate("spawnpoint/y", Number::class.java, spawnpoint)
                        validate("spawnpoint/z", Number::class.java, spawnpoint)
                        val world = if (player is Info) {
                            player.player.world
                        } else {
                            validate("spawnpoint/world", String::class.java, spawnpoint)
                            Bukkit.getWorld(spawnpoint["world"] as String)
                        }
                        val x = spawnpoint["x"].toString().toDouble()
                        val y = spawnpoint["y"].toString().toDouble()
                        val z = spawnpoint["z"].toString().toDouble()
                        Location(world, x, y, z)
                    }
                    is SimpleLocation -> SimpleLocation.recover(spawnpoint)
                    else -> throw IllegalArgumentException(getter["npc.error.wrongType", "spawnpoint"])
                }
            if (player is Info && spawnpointLocation.distance(player.player.location) > 7)
                throw IllegalArgumentException(getter["npc.error.spawnTooFar"])
            @Suppress("UNCHECKED_CAST")
            val onSpawn: Function<Any?, Any?>? = if (map["onSpawn"] is Function<*, *>) {
                map["onSpawn"]!! as Function<Any?, Any?>
            } else null
            val target = map["target"]
            val attack = map["attack"]

            return SimpleNPC.deserialize(
                schedule = script.schedule,
                getter = getter,
                name = name,
                spawnpoint = spawnpointLocation,
                onSpawn = onSpawn,
                target = target,
                attack = attack
            )
        } else {
            throw IllegalArgumentException(getter["npc.error.parNotFound", "name, spawnpoint"])
        }
    }

    override fun equals(other: Any?): Boolean = other is NPCUtils && other.getter == this.getter
    override fun hashCode(): Int {
        return getter.hashCode()
    }
}