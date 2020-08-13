package com.zhufu.opencraft.task

import com.zhufu.opencraft.Base
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.scheduler.BukkitTask
import kotlin.random.Random

abstract class ScheduledSpawnTask : SeparatedWorldTask() {
    override fun onStart() {
        super.onStart()
        scheduleMonsterSpawn()
    }

    override fun onStop() {
        super.onStop()
        spawnTask?.cancel()
    }

    private var spawnTask: BukkitTask? = null
    private fun scheduleMonsterSpawn() {
        val spawn = Runnable {
            fun spawn(chunk: Chunk, type: EntityType) {
                val x = random.nextDouble(16.0) + chunk.x * 16
                val z = random.nextDouble(16.0) + chunk.z * 16
                val location = Location(chunk.world, x, spawnHeight(x.toInt(), z.toInt()).toDouble(), z)
                chunk.world.spawnEntity(location, type).also { onSpawn(it) }
            }
            chunks.forEach {
                for (i in 0 until amountPerMin) {
                    var hasSpawned = false
                    var possibility = 0.0
                    for ((t, u) in typedRate) {
                        possibility += u
                        if (random.nextDouble() <= u) {
                            spawn(it, t)
                            hasSpawned = true
                            break
                        }
                    }
                    // If there isn't a mob spawning in this chunk and it must spawn.
                    if (!hasSpawned && possibility >= 1.0) {
                        spawn(it, typedRate.entries.maxBy { entry -> entry.value }!!.key)
                    }
                }
            }
        }
        spawnTask = Bukkit.getScheduler().runTaskTimer(Base.pluginCore, spawn, 0, 1200)
    }

    abstract val typedRate: Map<EntityType, Double>
    abstract val amountPerMin: Int
    abstract val chunks: List<Chunk>
    open val random: Random = Random.Default
    abstract fun spawnHeight(x: Int, z: Int): Int

    open fun onSpawn(entity: Entity){}
}