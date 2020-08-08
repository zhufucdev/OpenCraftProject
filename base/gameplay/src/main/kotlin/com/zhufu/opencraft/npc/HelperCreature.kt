package com.zhufu.opencraft.npc

import com.zhufu.opencraft.isMonster
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.ai.StuckAction
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter
import net.citizensnpcs.api.ai.tree.BehaviorStatus
import net.citizensnpcs.api.event.NPCDeathEvent
import net.citizensnpcs.api.event.NPCDespawnEvent
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Owner
import org.bukkit.Bukkit
import org.bukkit.entity.Creeper
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.plugin.Plugin
import java.util.*

class HelperCreature(private val npc: NPC, plugin: Plugin) : BehaviorGoalAdapter(), Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        npc.navigator.defaultParameters.stuckAction { _, _ -> false }
    }

    private var mTick: (() -> Unit)? = null

    @EventHandler
    fun onPlayerHurt(event: EntityDamageByEntityEvent) {
        if (event.entity.uniqueId == owner?.uniqueId // If owner is damaged
            || (event.entity.uniqueId == npc.entity?.uniqueId && event.damager.uniqueId != owner?.uniqueId) // If self is damaged and not by owner
        ) {
            mTick = NPCHelper.attack(npc, event.damager)
        }
    }

    @EventHandler
    fun onNPCDeath(event: NPCDeathEvent) {
        if (event.npc.uniqueId == npc.uniqueId) {
            event.droppedExp = 0
            event.drops.clear()

            //Unregister
            npc.despawn()
            npc.destroy()
        }
    }

    @EventHandler
    fun onSelfDespawn(event: NPCDespawnEvent) {
        if (event.npc.uniqueId == npc.uniqueId) {
            HandlerList.unregisterAll(this)
        }
    }

    private val navigator get() = npc.navigator
    private val owner get() = Bukkit.getPlayer(npc.data().get<UUID>("owner"))
    override fun run(): BehaviorStatus {
        val m = owner ?: return BehaviorStatus.FAILURE
        if (!shouldExecute()) return BehaviorStatus.SUCCESS
        if (!navigator.isNavigating || navigator.entityTarget?.target?.uniqueId == m.uniqueId) {
            val nearbyMonsters = m.location.getNearbyLivingEntities(13.0)
                .filter {
                    it.type.isMonster
                            && (!it.hasMetadata("NPC")
                            || CitizensAPI.getNamedNPCRegistry("temp").getNPC(it)?.data()?.has("owner") == false)
                }
            if (nearbyMonsters.isNotEmpty()) {
                val distanceDetector: (Entity) -> Double = { it.location.distance(m.location) }
                mTick = NPCHelper.attack(
                    npc = npc,
                    target = if (npc.entity.type == EntityType.ENDERMAN) nearbyMonsters.maxBy(distanceDetector)!!
                    else nearbyMonsters.minBy(distanceDetector)!!
                )
            } else {
                mTick = null
                navigator.setTarget(m, false)
                navigator.localParameters.distanceMargin(2.0)
            }
        }
        mTick?.invoke()
        return BehaviorStatus.RUNNING
    }

    override fun reset() {
        if (navigator.isNavigating)
            navigator.cancelNavigation()
    }

    override fun shouldExecute(): Boolean = owner != null && npc.entity != null && !npc.data().has("controller")
}