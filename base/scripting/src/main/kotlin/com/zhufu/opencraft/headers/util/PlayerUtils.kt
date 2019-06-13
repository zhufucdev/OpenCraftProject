package com.zhufu.opencraft.headers.util

import com.zhufu.opencraft.runSync
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity

object PlayerUtils {
    fun findByName(name: String) = Bukkit.getPlayer(name)
    fun findOfflineByName(name: String) = Bukkit.getOfflinePlayer(name)

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
}