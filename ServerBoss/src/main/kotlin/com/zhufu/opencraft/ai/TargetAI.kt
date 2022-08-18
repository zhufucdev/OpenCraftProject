package com.zhufu.opencraft.ai

import com.zhufu.opencraft.*
import com.zhufu.opencraft.traits.Equipments
import com.zhufu.opencraft.util.TextUtil
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter
import net.citizensnpcs.api.ai.tree.BehaviorStatus
import net.citizensnpcs.api.event.NPCDamageByEntityEvent
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Equipment
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.projectiles.ProjectileSource
import org.bukkit.util.Vector
import kotlin.math.roundToInt

class TargetAI(
    val npc: NPC,
    private val radius: Double,
    private val difficulty: Long,
    private val plugin: Plugin,
    private val spawnLocation: Location
) :
    BehaviorGoalAdapter(), Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    /*
    Properties
     */
    private var littleSpawnedCount = 0
    private var isLittleSpawned = false
    var healthGiver: NPC? = null
    private var isSpinner = false
    private var isSkeleton = false
    private var isArrowSphereShoot = false
    private var madness = 0
    private val damageByTarget = hashMapOf<Player, Double>()
    private val shotSpeed = NPCController.spinnerSpeedForCurrent()
    private val fireRate = NPCController.fireSpawnRateForCurrent()
    private val maxLittleSpawn = NPCController.littleBossMaxSpawnCount()
    private var tickShooting = 0
    override fun run(): BehaviorStatus {
        val navigator = npc.navigator
        val chunk = npc.entity.location.chunk
        if (!chunk.isLoaded) {
            chunk.load(true)
        }
        if (npc.entity.location.let { it.world == spawnLocation.world && it.distance(spawnLocation) >= radius * 3 }) {
            npc.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
            return BehaviorStatus.FAILURE
        }

        val target = target
        return when {
            target != null -> {
                if (target.world != npc.storedLocation.world) {
                    updateTarget()
                    return BehaviorStatus.FAILURE
                }
                if (target.isDead) {
                    updateTarget()
                    return BehaviorStatus.SUCCESS
                }

                val distanceToTarget = target.location.distance(npc.entity.location)
                if (distanceToTarget > radius) {
                    updateTarget()
                    if (this.target == null) {
                        this.target = target
                    } else
                        return BehaviorStatus.RUNNING
                }

                if (npc.entity.world == npc.storedLocation.world)
                    npc.entity.apply {
                        if (target.isFlying) {
                            NavigateUtility.targetFlyable(this, target)
                            if (distanceToTarget > radius * 0.3) {
                                NavigateUtility.dashTo(this, target.location, 0.2)
                            }
                        } else {
                            setGravity(true)
                        }
                    }

                if (isSpinner && distanceToTarget <= 6) {
                    if (tickShooting >= shotSpeed) {
                        fire()
                        tickShooting = 0
                    }
                    tickShooting++
                    BehaviorStatus.RUNNING
                } else {
                    if (navigator.entityTarget?.target != target) {
                        if (navigator.isNavigating) navigator.cancelNavigation()

                        navigator.setTarget(target, true)
                        NavigateUtility.dashTo(npc.entity, target.location, 0.3)
                    }
                    if (npc.entity.isInsideVehicle) npc.entity.leaveVehicle()
                    if (difficulty >= 50) {
                        if ((npc.entity as LivingEntity).healthRate() <= 0.2) {
                            if (!isLittleSpawned
                                && littleSpawnedCount < maxLittleSpawn
                            ) {
                                spawnLittle()
                                isLittleSpawned = true
                                littleSpawnedCount++
                            }
                        } else {
                            isLittleSpawned = false
                        }
                    }
                    if (difficulty >= 90) {
                        val entity = npc.entity as LivingEntity
                        if (entity.healthRate() <= 0.5 && !isArrowSphereShoot) {
                            isArrowSphereShoot = true
                            npc.entity.world.apply {
                                val location = entity.eyeLocation
                                fun makeStandard(arrow: Arrow) {
                                    arrow.shooter = entity
                                    arrow.damage = NPCController.arrowDamageForCurrent() * 0.7
                                }
                                for (i in 0..10) {
                                    for (k in 0..5) {
                                        makeStandard(
                                            spawnArrow(
                                                location,
                                                vector(i * 18.0, k * -18.0),
                                                NPCController.arrowSpeedForCurrent(),
                                                0F
                                            )
                                        )
                                        makeStandard(
                                            spawnArrow(
                                                location,
                                                vector(i * -18.0, k * -18.0),
                                                NPCController.arrowSpeedForCurrent(),
                                                0F
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    val oldMadness = madness
                    madness = getMadnessWith(target)
                    if (madness > oldMadness) {
                        val entity = npc.entity as LivingEntity
                        // Spawn particles
                        val location = entity.eyeLocation.add(Vector(0, 1, 0))
                        location.world.spawnParticle(Particle.VILLAGER_ANGRY, location, 20)
                        // Add fire aspect effect
                        entity.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.FIRE_RESISTANCE,
                                Int.MAX_VALUE,
                                1,
                                true,
                                true
                            )
                        )
                        if (madness > 1) {
                            // Add strength effect
                            entity.addPotionEffect(
                                PotionEffect(
                                    PotionEffectType.INCREASE_DAMAGE,
                                    Int.MAX_VALUE,
                                    1,
                                    true,
                                    true
                                )
                            )
                        }
                        if (madness > 2) {
                            navigator.localParameters.speedModifier(1.2F)
                        }
                    }
                    if (madness > 0 && showSpawnFire) {
                        spawnFire()
                    }
                    BehaviorStatus.RUNNING
                }
            }
            else -> BehaviorStatus.FAILURE
        }
    }

    private fun fire() {
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
    }

    override fun reset() {
        target = null
        isSpinner = false
        isArrowSphereShoot = false
        isLittleSpawned = false
        damageByTarget.clear()
        madness = 0
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
        val old = target
        val newTarget = npc.entity?.getNearbyEntities(radius, radius, radius)
            ?.firstOrNull { it.type == EntityType.PLAYER && !it.isInvulnerable }
                as Player?
        if (newTarget != null && newTarget != old) {
            damageByTarget[newTarget] = 0.0
            target = newTarget
        }
    }

    private fun spawnLittle() {
        val num = -1000 / (difficulty + 49) + 20
        val littleOneType =
            if (npc.entity.type == EntityType.SPIDER) EntityType.CAVE_SPIDER
            else npc.entity.type
        for (i in 0 until num) {
            npc.owningRegistry.createNPC(littleOneType, TextUtil.info("Little Boss # $i")).apply {
                isProtected = false
                isFlyable = true

                onSpawn {
                    if (npc.entity.type.name.contains("ZOMBIE") || npc.entity.type == EntityType.DROWNED)
                        (entity as Zombie).setBaby()
                    if (madness > 0)
                        (entity as LivingEntity).addPotionEffect(
                            PotionEffect(
                                PotionEffectType.FIRE_RESISTANCE,
                                Int.MAX_VALUE,
                                1,
                                true,
                                true
                            )
                        )
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

    private fun getMadnessWith(player: Player) =
        damageByTarget[player]
            ?.let {
                val maxHealth = (npc.entity as LivingEntity).getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue
                when {
                    it > maxHealth * 0.9 -> 3
                    it > maxHealth * 0.6 -> 2
                    it > maxHealth * 0.3 -> 1
                    else -> 0
                }
            } ?: 0

    private lateinit var lastFireSpawn: Location
    private val showSpawnFire
        get() =
            !::lastFireSpawn.isInitialized
                    || npc.entity.let { it.world != lastFireSpawn.world || it.location.distance(lastFireSpawn) >= radius }

    private fun spawnFire() {
        val base = npc.entity.location
        lastFireSpawn = base
        base.world.apply {
            spawnParticle(Particle.VILLAGER_ANGRY, (npc.entity as LivingEntity).eyeLocation.add(Vector(0, 1, 0)), 20)
            spawnParticle(Particle.LAVA, base, (radius * 10).toInt(), radius, 0.0, radius)
            playSound(base, Sound.BLOCK_WOOL_PLACE, 1f, 1f)
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
            Thread.sleep(400)
            val r = radius.toInt()
            for (x in -r..r)
                for (z in -r..r)
                    for (y in base.blockY until 256) {
                        val block = base.clone().add(Vector(x, y - base.blockY, z)).block
                        if (block.isEmpty || block.isPassable) {
                            if (Base.trueByPercentages(fireRate + (madness - 1) * 0.1F))
                                Bukkit.getScheduler().callSyncMethod(plugin) {
                                    block.type = Material.FIRE
                                }
                            break
                        } else if (block.isEmpty) {
                            break
                        }
                    }
        }
    }

    @EventHandler
    fun onNPCDamaged(event: NPCDamageByEntityEvent) {
        val damager = event.damager
        if (damager is Projectile && damager.shooter == event.npc.entity) {
            event.isCancelled = true
        } else if (event.npc == npc) {
            fun adjustDamage(player: Player) {
                val v = damageByTarget[player] ?: 0.0
                damageByTarget[player] = v + event.damage
            }
            if (damager is Player) {
                target = damager
                adjustDamage(damager)
            } else if (damager is Projectile && damager.shooter is Player) {
                target = damager.shooter as Player
                adjustDamage(damager.shooter as Player)
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (event.entity == target)
            updateTarget()
    }
}