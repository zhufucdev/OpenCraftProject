package com.zhufu.opencraft.ai

import com.zhufu.opencraft.NPCController
import com.zhufu.opencraft.healthRate
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter
import net.citizensnpcs.api.ai.tree.BehaviorStatus
import net.citizensnpcs.api.event.NPCDamageByEntityEvent
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import kotlin.math.abs

class LittleOneAI(private val parent: TargetAI, private val npc: NPC, plugin: Plugin) : BehaviorGoalAdapter(),
    Listener {
    private var target: Player? = null
    private var givenHealth = false

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    override fun run(): BehaviorStatus {
        return if (parent.npc.entity?.isDead == false && (parent.npc.entity as LivingEntity).healthRate() < 0.1) {
            npc.navigator.apply {
                if (entityTarget?.target != parent.npc.entity) {
                    if (isNavigating) cancelNavigation()
                    setTarget(parent.npc.entity, false)
                } else {
                    // When getting close to parent alive
                    if (!givenHealth && !parent.npc.entity.isDead && npc.entity.location.distance(parent.npc.entity.location) <= 3) {
                        if (parent.healthGiver == null) {
                            parent.healthGiver = npc

                            val healthier = npc.entity as LivingEntity
                            val parentEntity = parent.npc.entity as LivingEntity

                            healthier.world.apply {
                                spawnParticle(Particle.HEART, healthier.eyeLocation, 10)
                                spawnParticle(Particle.HEART, parentEntity.eyeLocation, 10)
                            }

                            Bukkit.getScheduler().runTaskLater(NPCController.mPlugin, { _ ->
                                if (parent.healthGiver == npc && !parentEntity.isDead) {
                                    // Give all it's health to parent
                                    parentEntity.apply {
                                        val newHealth = health + healthier.health
                                        val maxHealth = getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue
                                        health = if (maxHealth >= newHealth) {
                                            newHealth
                                        } else {
                                            maxHealth
                                        }
                                        healthier.damage(healthier.health)
                                        givenHealth = true
                                        parent.healthGiver = null
                                    }
                                }
                            }, 20)
                        }
                    }
                }
            }
            BehaviorStatus.RUNNING
        } else {
            when (target) {
                npc.navigator.entityTarget?.target -> {
                    if (target?.isDead == true)
                        return BehaviorStatus.SUCCESS
                    npc.entity.apply {
                        if (target?.isFlying == true) {
                            NavigateUtility.targetFlyable(this, target!!)
                        } else {
                            setGravity(true)
                        }
                    }
                    BehaviorStatus.RUNNING
                }
                null -> BehaviorStatus.SUCCESS
                else -> {
                    val navigator = npc.navigator
                    if (navigator.isNavigating) navigator.cancelNavigation()
                    navigator.setTarget(target, true)
                    BehaviorStatus.RUNNING
                }
            }
        }
    }

    override fun reset() {
        npc.navigator.cancelNavigation()
        target = null
    }

    override fun shouldExecute(): Boolean {
        if (!npc.isSpawned) return false
        return if (!npc.entity.isDead) {
            target = parent.target
            true
        } else {
            false
        }
    }

    @EventHandler
    fun onNPCDamaged(event: NPCDamageByEntityEvent) {
        val damager = event.damager
        if (event.npc == npc) {
            if (damager is Player)
                target = damager
            else if (damager is Projectile && damager.shooter is Player)
                target = damager.shooter as Player
        }
    }

    @EventHandler
    fun onTargetDeath(event: PlayerDeathEvent) {
        if (event.entity == target) {
            target = null
        }
    }
}