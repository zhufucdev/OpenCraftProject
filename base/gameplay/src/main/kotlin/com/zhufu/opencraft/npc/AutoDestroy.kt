package com.zhufu.opencraft.npc

import com.zhufu.opencraft.Base
import net.citizensnpcs.api.trait.Trait
import net.citizensnpcs.api.trait.TraitName
import net.citizensnpcs.api.util.DataKey
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.scheduler.BukkitTask

@TraitName("auto_destroy")
class AutoDestroy : Trait("auto_destroy") {
    private lateinit var timer: BukkitTask

    private var mDelay = 60 * 20L
    var delay: Long
        get() = mDelay
        set(value) {
            mDelay = value
            if (timerStart > 0) {
                val duration = System.currentTimeMillis() - timerStart
                startTimer(delay - duration / 50)
            }
        }

    private var timerStart = 0L
    override fun onAttach() {
        super.onAttach()
        startTimer(delay)
        timerStart = System.currentTimeMillis()
    }

    private fun startTimer(delay: Long) {
        if (::timer.isInitialized) timer.cancel()

        timer = Bukkit.getScheduler().runTaskLater(Base.pluginCore, Runnable {
            npc.entity?.world?.spawnParticle(Particle.WHITE_ASH, npc.entity.location, 1)
            npc.despawn()
            npc.destroy()
        }, delay)
    }

    override fun save(key: DataKey) {
        super.save(key)
        key.setLong("delay", mDelay)
        key.setLong("start", timerStart)
    }

    override fun load(key: DataKey?) {
        super.load(key)
        if (key == null) return
        mDelay = key.getLong("delay")
        timerStart = key.getLong("start")
    }
}