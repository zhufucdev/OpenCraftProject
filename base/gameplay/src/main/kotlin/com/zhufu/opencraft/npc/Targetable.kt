package com.zhufu.opencraft.npc

import com.zhufu.opencraft.Base
import net.citizensnpcs.api.trait.Trait
import net.citizensnpcs.api.trait.TraitName
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.scheduler.BukkitTask

@TraitName("targetable")
class Targetable: Trait("targetable") {
    private var task: BukkitTask? = null
    override fun onSpawn() {
        super.onSpawn()
        task = Bukkit.getScheduler().runTaskTimer(Base.pluginCore, Runnable {
            val entity = npc.entity
            if (entity !is LivingEntity) return@Runnable
            npc.entity.location.getNearbyLivingEntities(10.0).forEach {
                if (it is Monster && it.target == null) {
                    it.target = entity
                }
            }
        }, 0, 10)
    }
}