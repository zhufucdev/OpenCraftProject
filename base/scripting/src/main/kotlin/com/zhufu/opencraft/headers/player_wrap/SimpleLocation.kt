package com.zhufu.opencraft.headers.player_wrap

import org.bukkit.Location
import org.bukkit.entity.Entity

@Suppress("unused")
class SimpleLocation private constructor(private val wrap: Location) {
    val x get() = wrap.x
    val y get() = wrap.y
    val z get() = wrap.z
    val world get() = wrap.world.name
    val yaw get() = wrap.yaw
    val pitch get() = wrap.pitch
    fun add(x: Double, y: Double, z: Double) = from(wrap.clone().add(x, y, z))

    val entitiesNearby
        get() = arrayListOf<SimpleEntity>().apply {
            wrap.getNearbyEntities(5.0, 5.0, 5.0).forEach { add(SimpleEntity.from(it)) }
        }.toTypedArray()

    fun getEntitiesNearby(radius: Double) = arrayListOf<SimpleEntity>().apply {
        wrap.getNearbyEntities(radius, radius, radius).forEach {
            add(SimpleEntity.from(it))
        }
    }.toTypedArray()
    fun getEntitiesNearby(type: String, radius: Double) =
        getEntitiesNearby(radius).filter { it.type.equals(type, true) }

    override fun toString(): String = "${this::class.simpleName}{world=$world,x=$x,y=$y,z=$z}"

    companion object {
        fun from(location: Location) = SimpleLocation(location)
        fun recover(sl: SimpleLocation) = sl.wrap
    }
}