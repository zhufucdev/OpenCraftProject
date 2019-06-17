package com.zhufu.opencraft.headers.player_wrap

import com.google.common.cache.CacheBuilder
import org.bukkit.entity.Entity
import java.util.*

@Suppress("unused")
class SimpleEntity private constructor(private val wrap: Entity) {
    val location get() = SimpleLocation.from(wrap.location)
    val velocity get() = wrap.velocity
    val height get() = wrap.height
    val width get() = wrap.width
    fun isOnGround() = wrap.isOnGround
    val world get() = wrap.world.name
    fun getNearby(x: Double, y: Double, z: Double): List<SimpleEntity> {
        val r = arrayListOf<SimpleEntity>()
        wrap.getNearbyEntities(x, y, z).forEach {
            r.add(from(it))
        }
        return r
    }

    val uuid get() = wrap.uniqueId
    val id get() = wrap.entityId
    fun isDead() = wrap.isDead
    fun isValid() = wrap.isValid
    fun isPersistent() = wrap.isPersistent
    val passengers: List<Entity> get() = wrap.passengers
    fun isSitting() = wrap.isInsideVehicle
    val vehicle get() = wrap.vehicle
    fun isGlowing() = wrap.isGlowing
    fun isInvulnerable() = wrap.isInvulnerable
    fun isSilent() = wrap.isSilent
    fun hasGravity() = wrap.hasGravity()

    val type = wrap.javaClass.simpleName.toLowerCase()

    override fun toString(): String = "${this::class.simpleName}{type=$type,location=$location}"

    companion object {
        private val cache = CacheBuilder.newBuilder().maximumSize(50).build<UUID, SimpleEntity>()
        fun from(entity: Entity) = cache[entity.uniqueId, {
            SimpleEntity(
                entity
            )
        }]
        fun recover(se: SimpleEntity) = se.wrap
    }
}