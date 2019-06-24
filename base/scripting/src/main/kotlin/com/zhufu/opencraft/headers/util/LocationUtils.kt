package com.zhufu.opencraft.headers.util

import com.zhufu.opencraft.headers.player_wrap.SimpleLocation
import org.bukkit.Bukkit
import org.bukkit.Location

@Suppress("unused")
object LocationUtils {
    fun of(world: String, x: Int, y: Int, z: Int) =
        Location(Bukkit.getWorld(world), x.toDouble(), y.toDouble(), z.toDouble())
    fun of(world: String, x: Double, y: Double, z: Double) = Location(Bukkit.getWorld(world), x, y, z)

    fun getSimply(location: Location) = SimpleLocation.from(location)

    fun getWorld(name: String) = Bukkit.getWorld(name)
}