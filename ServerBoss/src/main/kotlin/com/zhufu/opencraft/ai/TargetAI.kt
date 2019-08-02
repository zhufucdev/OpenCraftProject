package com.zhufu.opencraft.ai

import com.zhufu.opencraft.*
import com.zhufu.opencraft.traits.Equipments
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter
import net.citizensnpcs.api.ai.tree.BehaviorStatus
import net.citizensnpcs.api.event.NPCDamageByEntityEvent
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Equipment
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.projectiles.ProjectileSource
import kotlin.math.roundToInt

class TargetAI(val npc: NPC, private val radius: Double, private val difficulty: Long, plugin: Plugin) :
    BehaviorGoalAdapter(), Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    private var isLittleSpawned = false
    var healthGiver: NPC? = null
    private var isSpinner = false
    private var isSkeleton = false
    private val speed = NPCController.spinnerSpeedForCurrent()
    private var tick = 0
    override fun run(): BehaviorStatus {
        val navigator = npc.navigator
        return when {
            target != null -> {
                if (target!!.world != npc.storedLocation.world) {
                    updateTarget()
                    return BehaviorStatus.FAILURE
                }
                if (isSpinner && target!!.location.distance(npc.entity.location) <= 6) {
                    if (tick >= speed) {
                        npc.entity.world.apply {
                            fun direct() = target!!.location.toVector().subtract(npc.entity.location.toVector())
                            val location = (npc.entity as LivingEntity).eyeLocation
                            val spawn = if (!isSkeleton) {
                                (spawnEntity(location, EntityType.FIREBALL) as Fireball).apply {
                                    direction = direct()
                                }
                            } else {
                                spawnArrow(
                                    location,
                                    direct(),
                                    NPCController.arrowSpeedForCurrent(),
                                    NPCController.arrowSpreadForCurrent()
                                ).apply {
                                    damage = NPCController.arrowDamageForCurrent()
                                }
                            }
                            spawn.shooter = npc.entity as ProjectileSource
                        }
                        tick = 0
                    }
                    tick++
                    BehaviorStatus.RUNNING
                } else {
                    if (navigator.entityTarget?.target != target) {
                        if (navigator.isNavigating) navigator.cancelNavigation()

                        navigator.setTarget(target!!, true)
                        npc.entity.velocity =
                            target!!.location.toVector().subtract(npc.entity.location.toVector()).multiply(0.3)
                    }
                    if (difficulty >= 50
                        && !isLittleSpawned
                        && (npc.entity as LivingEntity).healthRate() <= 0.2
                    ) {
                        spawnLittle()
                        isLittleSpawned = true
                    }
                    BehaviorStatus.RUNNING
                }
            }
            target == null -> BehaviorStatus.SUCCESS
            else -> BehaviorStatus.RUNNING
        }
    }

    override fun reset() {
        target = null
        isSpinner = false
        isSkeleton = false
        npc.navigator.cancelNavigation()
    }

    override fun shouldExecute(): Boolean {
        if (!npc.isSpawned) return false
        return if (!npc.entity.isDead) {
            updateTarget()
            isSpinner = listOf(EntityType.BLAZE, EntityType.SKELETON).contains(npc.entity.type)
            if (isSpinner) {
                isSkeleton = npc.entity.type == EntityType.SKELETON
                npc.navigator.defaultParameters.distanceMargin(NPCController.radiusForCurrent())
            } else {
                npc.navigator.defaultParameters.distanceMargin(2.0)
            }
            true
        } else {
            false
        }
    }

    var target: Player? = null
    private fun updateTarget() {
        target = npc.entity?.getNearbyEntities(radius, radius, radius)?.firstOrNull { it.type == EntityType.PLAYER }
                as Player?
    }

    private fun spawnLittle() {
        val num = -1000 / (difficulty + 49) + 20
        val littleOneType =
            if (npc.entity.type == EntityType.SPIDER) EntityType.CAVE_SPIDER
            else npc.entity.type
        for (i in 0 until num) {
            npc.owningRegistry.createNPC(littleOneType, "Little Boss # $i".toInfoMessage()).apply {
                isProtected = false

                if (npc.entity.type.name.contains("ZOMBIE") || npc.entity.type == EntityType.DROWNED)
                    onSpawn {
                        (entity as Zombie).isBaby = true
                    }
                addTrait(Equipment().apply {
                    equipment[0] = ItemStack(Material.IRON_SWORD).apply {
                        addUnsafeEnchantment(
                            Enchantment.DAMAGE_ALL,
                            (NPCController.weaponDamageLevelFor(difficulty) / 2).roundToInt()
                        )
                    }
                    equipment[Equipments.HELMET.index] = ItemStack(Material.IRON_HELMET)
                })
                data()["little"] = true
                defaultGoalController.addBehavior(LittleOneAI(this@TargetAI, this, NPCController.mPlugin), 1)

                spawn(
                    npc.entity.location.clone().add(Base.random(3), 0.0, Base.random(3))
                )
            }
        }
    }

    @EventHandler
    fun onNPCDamaged(event: NPCDamageByEntityEvent) {
        val damager = event.damager
        if (damager is Projectile && damager.shooter == event.npc.entity) {
            event.isCancelled = true
        } else if (event.npc == npc) {
            if (damager is Player)
                target = damager
            else if (damager is Projectile && damager.shooter is Player)
                target = damager.shooter as Player
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        updateTarget()
    }
}