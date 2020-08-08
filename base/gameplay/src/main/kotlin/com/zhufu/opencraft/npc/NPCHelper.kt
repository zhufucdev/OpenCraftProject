package com.zhufu.opencraft.npc

import com.zhufu.opencraft.*
import com.zhufu.opencraft.Base.TutorialUtil.linearTo
import com.zhufu.opencraft.events.PlayerLogoutEvent
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.ai.tree.IfElse
import net.citizensnpcs.api.event.NPCDamageEvent
import net.citizensnpcs.api.event.NPCDeathEvent
import net.citizensnpcs.api.event.NPCDespawnEvent
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Equipment
import net.citizensnpcs.api.trait.trait.Spawned
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.math.roundToInt

object NPCHelper {
    fun attack(npc: NPC, target: Entity): (() -> Unit)? {
        val navigator = npc.navigator
        if (navigator.isNavigating) navigator.cancelNavigation()
        navigator.setTarget(target, true)
        return when (npc.entity.type) {
            EntityType.CREEPER -> {
                val entity = npc.entity as Creeper
                return {
                    if (npc.entity.location.distance(target.location) <= entity.explosionRadius) {
                        navigator.cancelNavigation()
                        entity.ignite()
                    }
                }
            }
            EntityType.BLAZE -> {
                navigator.defaultParameters.distanceMargin(4.0)
                return {
                    if (npc.entity.location.distance(target.location) <= 4.5) {
                        val direction = target.location.toVector().subtract(npc.entity.location.toVector()).unitVector()
                        npc.entity.world.spawn(npc.entity.location.add(direction), Fireball::class.java)
                            .direction = direction
                    }
                }
            }
            EntityType.ENDERMAN -> {
                return {
                    if (target.location.distance(npc.entity.location) >= 3) {
                        npc.entity.teleport(target.location)
                    }
                }
            }
            else -> null
        }
    }

    fun createSummonAI(npc: NPC, plugin: Plugin): IfElse =
        IfElse.create(
            { npc.data().get<UUID?>("controller") != null },
            ControlledCreature(npc, plugin),
            HelperCreature(npc, plugin)
        )

    private val controlMap = hashMapOf<Info, NPCControl>()
    fun control(npc: NPC, controller: Info, plugin: Plugin): NPCControl =
        controlMap[controller] ?: NPCControl(controller, npc, plugin).also { controlMap[controller] = it }

    fun controlling(examine: Info) = controlMap.containsKey(examine)

    class NPCControl internal constructor(val controller: Info, val npc: NPC, private val plugin: Plugin) : Listener {
        val puppet: NPC = CitizensAPI.getNamedNPCRegistry("temp").createNPC(EntityType.PLAYER, controller.name)
        private var started = false

        init {
            Bukkit.getPluginManager().registerEvents(this, plugin)
            controlMap[controller] = this
        }

        private fun attack(other: Entity? = null) {
            when (npc.entity.type) {
                EntityType.CREEPER -> {
                    val entity = npc.entity as Creeper
                    entity.ignite()
                    Bukkit.getScheduler().runTaskLater(Base.pluginCore, { _ ->
                        destroy()
                    }, entity.maxFuseTicks.toLong())
                }
                EntityType.BLAZE -> controller.player.location.apply {
                    world.spawn(this.add(direction), Fireball::class.java)
                        .direction = direction
                }
                else -> if (other != null) (npc.entity as LivingEntity).attack(other)
            }
        }

        fun start() {
            if (started) return

            npc.data().set("controller", controller.uuid)

            // Clone the player
            val player = controller.player
            puppet.apply {
                addTrait(Equipment().apply {
                    equipment[1] = player.inventory.helmet?.clone()
                    equipment[2] = player.inventory.chestplate?.clone()
                    equipment[3] = player.inventory.leggings?.clone()
                    equipment[4] = player.inventory.boots?.clone()
                    equipment[0] = player.inventory.itemInMainHand.clone()
                    equipment[5] = player.inventory.itemInOffHand.clone()
                })
                addTrait(Targetable())
                addTrait(Movable())
                isProtected = false
                spawn(player.location)
            }
            // Adjust player settings
            controller.inventory.create(DualInventory.NOTHING).load()
            player.apply {
                inventory.clear()
                gameMode = GameMode.ADVENTURE
                foodLevel = 20
                isInvulnerable = true
                addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, 1, false, false, false))
                addPotionEffect(PotionEffect(PotionEffectType.SATURATION, Int.MAX_VALUE, 1, false, false, false))
                val type = npc.entity.type
                if (type == EntityType.ENDERMAN)
                    inventory.setItem(0, ItemStack(Material.ENDER_PEARL, 16))
                if (type == EntityType.IRON_GOLEM)
                    addPotionEffect(PotionEffect(PotionEffectType.SLOW, Int.MAX_VALUE, 1, false, false, false))
                showHP()
            }

            started = true
        }

        fun stop() {
            if (!started) return
            Bukkit.getScheduler().runTask(plugin) { _ ->
                puppet.despawn()
                puppet.destroy()
            }

            npc.data().remove("controller")

            controller.player.apply {
                isInvulnerable = false
                activePotionEffects.forEach { removePotionEffect(it.type) }
                giveExp(0)
            }
            controller.inventory.last.apply {
                set("location", puppet.entity.location)
                load()
            }
            started = false
        }

        fun destroy() {
            stop()
            HandlerList.unregisterAll(this)
            controlMap.remove(controller)
        }

        @EventHandler
        fun onNPCDeath(event: NPCDeathEvent) {
            if (!started) return
            if (event.npc.uniqueId == npc.uniqueId) {
                destroy()
            } else if (event.npc.uniqueId == puppet.uniqueId) {
                destroy()
                Bukkit.getScheduler().runTask(Base.pluginCore) { _ ->
                    controller.player.health = 0.0
                }
            }
        }

        @EventHandler
        fun onNPCDespawn(event: NPCDespawnEvent) {
            if (!started) return
            if (event.npc.uniqueId == npc.uniqueId) {
                destroy()
            }
        }

        @EventHandler
        fun onPlayerLogout(event: PlayerLogoutEvent) {
            if (!started) return
            if (event.info == controller) {
                destroy()
            }
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerInteractEvent) {
            if (!started) return
            if (event.player.uniqueId != controller.uuid) return
            if (npc.entity?.type == EntityType.ENDERMAN && event.hasItem()) return // Ignore Ender Pearl teleport
            if (event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.RIGHT_CLICK_BLOCK) {
                destroy()
            } else if (event.action == Action.LEFT_CLICK_BLOCK || event.action == Action.LEFT_CLICK_AIR) {
                attack()
            }
        }

        @EventHandler
        fun onAttack(event: EntityDamageByEntityEvent) {
            if (!started) return
            if (event.damager.uniqueId != controller.uuid) return
            event.isCancelled = true
            attack(event.entity)
        }

        private fun showHP() {
            val entity = npc.entity as LivingEntity
            controller.player.apply {
                sendExperienceChange(
                    (entity.health / entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value).toFloat(),
                    entity.health.roundToInt()
                )
            }
        }

        @EventHandler
        fun onNPCDamage(event: EntityDamageEvent) {
            if (!event.entity.hasMetadata("NPC")) return
            val npc2 = CitizensAPI.getNamedNPCRegistry("temp").getNPC(event.entity)
            if (npc2.uniqueId != npc.uniqueId) return
            showHP()
        }

        @EventHandler
        fun onPlayerUsePearl(event: PlayerTeleportEvent) {
            if (!started) return
            if (npc.entity?.type != EntityType.ENDERMAN) return
            if (event.player.uniqueId != controller.uuid || event.cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL)
                return
            event.player.inventory.setItem(0, ItemStack(Material.ENDER_PEARL, 16))
        }
    }
}