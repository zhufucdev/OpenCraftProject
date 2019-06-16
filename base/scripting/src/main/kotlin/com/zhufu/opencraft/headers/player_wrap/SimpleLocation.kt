package com.zhufu.opencraft.headers.player_wrap

import org.bukkit.Location

@Suppress("unused")
class SimpleLocation private constructor(private val wrap: Location){
    val x get() = wrap.x
    val y get() = wrap.y
    val z get() = wrap.z
    val world get() = wrap.world.name
    val yaw get() = wrap.yaw
    val pitch get() = wrap.pitch
    fun add(x: Double,y: Double,z: Double) = SimpleLocation.from(wrap.clone().add(x, y, z))

    companion object {
        fun from(location: Location) = SimpleLocation(location)
        fun recover(sl: SimpleLocation) = sl.wrap
    }
}