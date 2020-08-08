package com.zhufu.opencraft.npc

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter
import net.citizensnpcs.api.ai.tree.BehaviorStatus
import net.citizensnpcs.api.event.NPCDespawnEvent
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import java.util.*

class ControlledCreature(private val npc: NPC, plugin: Plugin): BehaviorGoalAdapter(), Listener {
    private val controller: Player? get() {
        return Bukkit.getPlayer(npc.data().get<UUID?>("controller") ?: return null)
    }
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onNPCDespawn(event: NPCDespawnEvent) {
        if (event.npc.uniqueId == npc.uniqueId) {
            HandlerList.unregisterAll(this)
        }
    }

    @EventHandler
    fun onPlayerJump(event: PlayerJumpEvent) {
        if (event.player.uniqueId == controller?.uniqueId) {
            npc.entity.velocity = Vector(0.0, 0.4, 0.0)
        }
    }

    override fun run(): BehaviorStatus {
        val controller = controller ?: return BehaviorStatus.FAILURE
        if (!shouldExecute()) return BehaviorStatus.SUCCESS

        val testLocation = npc.entity.location
        while (testLocation.block.type.let { !it.isAir && it.isOccluding }) {
            testLocation.add(0.0, 1.0, 0.0)
        }
        npc.entity.teleport(controller.location.subtract(controller.location.direction).apply { y = testLocation.y })
        controller.location.getNearbyLivingEntities(10.0).forEach {
            if (it is Mob && it.target?.uniqueId == controller.uniqueId) {
                it.target = npc.entity as LivingEntity
            }
        }
        return BehaviorStatus.RUNNING
    }

    override fun reset() {
    }

    override fun shouldExecute(): Boolean = controller != null && npc.entity != null
}