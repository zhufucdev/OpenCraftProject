package com.zhufu.opencraft

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.FallingBlock
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector

object ShootingWebHandler {
    private const val LIVING_LIMIT = 100

    private lateinit var plugin: Plugin
    private var ticking: BukkitTask? = null
    fun init(plugin: Plugin) {
        this.plugin = plugin
    }

    private val movingDirection = hashMapOf<FallingBlock, Vector>()

    fun spawn(from: Location, direction: Vector) {
        from.world.spawnFallingBlock(from, Material.COBWEB.createBlockData())
            .apply {
                setHurtEntities(true)
                setGravity(false)
                velocity = direction.normalize()
                movingDirection[this] = velocity
            }
        tick()
    }

    private fun tick() {
        if (ticking != null) return
        ticking = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            movingDirection.entries.toList().forEach { e ->
                val l = e.key.location.toVector()
                val entities = e.key.location.getNearbyLivingEntities(1.0, 1.0, 1.0)
                val hits = entities.any {
                    it.location
                        .toVector()
                        .subtract(l)
                        .normalize()
                        .subtract(e.value)
                        .length() < 0.5
                }
                if (hits
                    || e.key.ticksLived >= LIVING_LIMIT
                    || e.key.velocity.length() <= 0.2) {
                    l.toLocation(e.key.world, 0F, 0F).block.type = Material.COBWEB
                    e.key.remove()
                    movingDirection.remove(e.key)
                    if (movingDirection.isEmpty())
                        stopTicking()
                }
            }
        }, 0, 1)
    }

    private fun stopTicking() {
        ticking?.cancel()
        ticking = null
    }
}