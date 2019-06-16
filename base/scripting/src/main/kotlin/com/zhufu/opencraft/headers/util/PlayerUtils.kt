package com.zhufu.opencraft.headers.util

import com.zhufu.opencraft.headers.player_wrap.PlayerSelf
import com.zhufu.opencraft.runSync
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.util.Vector

@Suppress("unused")
object PlayerUtils {
    fun findByName(name: String) = Bukkit.getPlayer(name)
    fun findOfflineByName(name: String) = Bukkit.getOfflinePlayer(name)

    fun teleport(who: PlayerSelf, where: Location) = runSync { who.player!!.teleport(where) }
    fun teleport(who: PlayerSelf, x: Int, y: Int, z: Int) = runSync {
        who.player!!.apply { teleport(Location(location.world, x.toDouble(), y.toDouble(), z.toDouble())) }
    }

    fun teleport(who: PlayerSelf, world: World, x: Int, y: Int, z: Int) = runSync {
        who.player!!.apply { teleport(Location(world, x.toDouble(), y.toDouble(), z.toDouble())) }
    }

    fun teleport(who: PlayerSelf, to: Entity) = runSync {
        who.player!!.teleport(to)
    }

    fun teleport(who: PlayerSelf, where: Vector) = runSync { who.player!!.apply { teleport(where.toLocation(world)) } }

    fun forward(who: PlayerSelf,x: Double,y: Double,z: Double) = runSync { who.player!!.apply { teleport(location.clone().add(x, y, z)) } }

    fun teleport(who: String, where: Location) = runSync { findByName(who)!!.teleport(where) }
    fun teleport(who: String, x: Int, y: Int, z: Int) = runSync {
        findByName(who)!!.apply { teleport(Location(location.world, x.toDouble(), y.toDouble(), z.toDouble())) }
    }

    fun teleport(who: String, world: World, x: Int, y: Int, z: Int) = runSync {
        findByName(who)!!.apply { teleport(Location(world, x.toDouble(), y.toDouble(), z.toDouble())) }
    }

    fun teleport(who: String, to: Entity) = runSync {
        findByName(who)!!.teleport(to)
    }

    fun teleport(who: String, where: Vector) = runSync { findByName(who)!!.apply { teleport(where.toLocation(world)) } }
    fun forward(who: String,x: Double,y: Double,z: Double) = runSync { findByName(who)!!.apply { teleport(location.clone().add(x, y, z)) } }
}