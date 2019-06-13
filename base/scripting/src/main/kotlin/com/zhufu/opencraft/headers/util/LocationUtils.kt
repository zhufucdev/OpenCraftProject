package com.zhufu.opencraft.headers.util

import org.bukkit.Bukkit
import org.bukkit.Location

object LocationUtils {
    fun fromPos(world: String, x: Int, y: Int, z: Int) =
        Location(Bukkit.getWorld(world), x.toDouble(), y.toDouble(), z.toDouble())

    fun fromPos(world: String, x: Double, y: Double, z: Double) = Location(Bukkit.getWorld(world), x, y, z)

    fun getWorld(name: String) = Bukkit.getWorld(name)
}