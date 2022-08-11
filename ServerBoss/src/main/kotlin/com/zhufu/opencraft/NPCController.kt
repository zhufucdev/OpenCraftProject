package com.zhufu.opencraft

import com.zhufu.opencraft.Base.random
import com.zhufu.opencraft.ai.TargetAI
import com.zhufu.opencraft.data.OfflineInfo
import com.zhufu.opencraft.traits.Equipments
import com.zhufu.opencraft.util.TextUtil
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.NPCDamageByEntityEvent
import net.citizensnpcs.api.event.NPCDeathEvent
import net.citizensnpcs.api.event.NPCSpawnEvent
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Equipment
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

object NPCController : Listener {
    private val BOSS_BAR_NAMESPACE by lazy { NamespacedKey(mPlugin, "boss_bar") }

    lateinit var currentNPC: NPC
    lateinit var currentType: EntityType
    lateinit var currentBossBar: BossBar
    var difficulty: Long = 0
    var isCurrentBossAlive: Boolean = false
        private set
    var withOutEqu = false
        private set
    lateinit var mPlugin: Plugin

    fun init(plugin: Plugin) {
        mPlugin = plugin
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun close() {
        if (isCurrentBossAlive) {
            currentNPC.destroy()
            Bukkit.removeBossBar(BOSS_BAR_NAMESPACE)
        }
        CitizensAPI.getNPCRegistry().forEach {
            if (it.data().get<Boolean?>("little") == true) {
                it.destroy()
            }
        }
    }

    fun spawn(difficulty: Long) {
        if (isCurrentBossAlive) {
            currentNPC.despawn()
            currentBossBar.removeAll()
        }
        this.difficulty = difficulty
        damageMap.clear()
        totalDamage = 0F

        currentType = when (random.nextInt(7)) {
            0 -> EntityType.ZOMBIE
            1 -> EntityType.SKELETON
            2 -> EntityType.PIGLIN
            3 -> {
                withOutEqu = true
                EntityType.SPIDER
            }
            4 -> EntityType.DROWNED
            5 -> {
                withOutEqu = true
                EntityType.BLAZE
            }
            else -> EntityType.WITHER_SKELETON
        }
        currentNPC = CitizensAPI.getNPCRegistry().createNPC(currentType, TextUtil.error("Server Boss # $difficulty"))
        var spawnLocation =
            Base.getRandomLocation(Base.surviveWorld, 100000, y = 256)

        fun test() {
            while (spawnLocation.block.type.isEmpty) {
                spawnLocation.add(Vector(0, -1, 0))
            }
            if (spawnLocation.block.isLiquid) {
                spawnLocation = Base.getRandomLocation(Base.surviveWorld, 100000, y = 256)
                test()
            }
        }
        test()
        spawnLocation.apply {
            add(Vector(0, 2, 0))
            chunk.isForceLoaded = true
            chunk.load(true)
        }

        currentNPC.apply {
            if (!withOutEqu) {
                addTrait(equipmentForCurrent())
            }
            onSpawn {
                if (withOutEqu) {
                    (entity as LivingEntity).apply {
                        val maxHealth = healthForSpecial()
                        getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue = maxHealth
                        health = maxHealth
                    }
                }

                broadcast("boss.spawned.1", TextUtil.TextColor.RED)
                broadcast(
                    "boss.spawned.2",
                    TextUtil.TextColor.WHITE,
                    spawnLocation.blockX.toString(),
                    spawnLocation.blockY.toString(),
                    spawnLocation.blockZ.toString()
                )
            }
            defaultGoalController.apply {
                addBehavior(
                    TargetAI(currentNPC, radiusForCurrent(), difficulty, mPlugin, spawnLocation),
                    1
                )
            }
            isProtected = false
            isFlyable = true
            spawn(spawnLocation)
        }

        isCurrentBossAlive = true

        currentBossBar = Bukkit.createBossBar(
            BOSS_BAR_NAMESPACE,
            TextUtil.error("ServerBoss # $difficulty"),
            BarColor.RED,
            BarStyle.SEGMENTED_12
        )
        Bukkit.getScheduler().runTaskTimer(mPlugin, { task ->
            Bukkit.getOnlinePlayers().forEach {
                if (!currentBossBar.players.contains(it)) {
                    currentBossBar.addPlayer(it)
                }
            }
            if (currentNPC.entity?.isDead == false) {
                currentBossBar.apply {
                    progress = (currentNPC.entity as LivingEntity).healthRate()
                }
            } else {
                Bukkit.removeBossBar(BOSS_BAR_NAMESPACE)
                task.cancel()
                currentBossBar.removeAll()
            }
        }, 0, 5)
    }

    private fun equipmentForCurrent(): Equipment {
        val r = Equipment()
        val protectionVal = (-1026.0 / (difficulty + 42.0 / 50) + 300).roundToInt()
        val thornsVal = (-1000.0 / (difficulty + 124) + 20).roundToInt()
        fun getEnchant(item: ItemStack): ItemStack {
            item.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, protectionVal)
            if (Base.trueByPercentages(0.5F)) {
                item.addUnsafeEnchantment(Enchantment.THORNS, thornsVal)
            }
            return item
        }

        fun set(lessWeight: String, moreWeight: String, p: Float) {
            fun takeLessWeight() = Base.trueByPercentages(p)

            Equipments.values().forEach {
                val root = it.name
                if (root.contains("HAND"))
                    return@forEach
                val name = "${if (takeLessWeight()) lessWeight else moreWeight}_$root"
                r.equipment[it.index] = getEnchant(ItemStack(Material.getMaterial(name)!!))
            }
        }
        when {
            difficulty < 10 ->
                set("CHAINMAIL", "LEATHER", difficulty / 10F)
            difficulty < 30 ->
                set("IRON", "CHAINMAIL", (difficulty - 10) / 20F)
            difficulty < 50 ->
                set("IRON", "GOLDEN", (difficulty - 30) / 20F)
            difficulty < 70 ->
                set("GOLDEN", "DIAMOND", (difficulty - 50) / 20F)
            else ->
                set("DIAMOND", "DIAMOND", 0F)
        }
        r.equipment[0] = getWeaponForCurrent()
        return r
    }

    fun weaponDamageLevelFor(difficulty: Long) = -600.0 / (difficulty + 23) + 25
    private fun getWeaponForCurrent(cut: Double = 1.0): ItemStack {
        fun set(material: Material) = ItemStack(material).apply {
            fun take() = Base.trueByPercentages(-1F / difficulty + 1)
            if (type.name.contains("SWORD")) {
                addUnsafeEnchantment(Enchantment.DAMAGE_ALL, (weaponDamageLevelFor(difficulty) * cut).roundToInt())

                if (take())
                    addUnsafeEnchantment(
                        Enchantment.KNOCKBACK,
                        (weaponDamageLevelFor(difficulty) * cut * 0.4).roundToInt()
                    )
                if (take())
                    addUnsafeEnchantment(
                        Enchantment.FIRE_ASPECT,
                        ((-1000.0 / (difficulty + 124) + 8) * cut).roundToInt()
                    )
            } else if (type == Material.BOW) {
                addUnsafeEnchantment(
                    Enchantment.ARROW_DAMAGE,
                    (weaponDamageLevelFor(difficulty) * cut * 0.6).roundToInt()
                )

                if (take())
                    addUnsafeEnchantment(
                        Enchantment.ARROW_KNOCKBACK,
                        (weaponDamageLevelFor(difficulty) * cut * 0.4).roundToInt()
                    )
                if (take())
                    addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1)
            }
        }

        return if (currentType == EntityType.SKELETON) {
            set(Material.BOW)
        } else {
            when {
                difficulty < 10 ->
                    set(Material.STONE_SWORD)
                difficulty < 30 ->
                    set(Material.GOLDEN_SWORD)
                difficulty < 50 ->
                    set(Material.IRON_SWORD)
                else ->
                    set(Material.DIAMOND_SWORD)
            }
        }
    }

    fun currencyForCurrent() = -250000.0 / (difficulty + 2451.0 / 49) + 5000
    fun healthForSpecial() = -20000.0 / (difficulty + 39) + 500
    fun arrowSpeedForCurrent() = -100 / (difficulty + 99) + 1.6F
    fun arrowSpreadForCurrent() = 1002 / (difficulty + 166F) + 6
    fun spinnerSpeedForCurrent() = 1025 / (difficulty + 40F)
    fun percentageToDropWeapon() = -100F / (difficulty + 249) + 0.8F
    fun percentageToDropEqui() = -100F / (difficulty + 999) + 0.2F
    fun radiusForCurrent() = sin(difficulty / 3.0) + 16
    fun arrowDamageForCurrent() = -1200.0 / (difficulty + 299) + 6
    fun littleBossMaxSpawnCount() = -1008.0 / (difficulty + 111) + 10
    fun expForCurrent() = -10000.0 / (difficulty - 9.0 / 59) + 12000
    fun fireSpawnRateForCurrent() = -56F / (difficulty + 79) + 0.8F

    val spawnListeners = HashMap<NPC, () -> Unit>()
    @EventHandler
    fun onBossSpawn(event: NPCSpawnEvent) {
        spawnListeners[event.npc]?.invoke()
    }

    private val damageMap = HashMap<Player, Double>()
    var totalDamage = 0F
    @EventHandler
    fun onBossDamaged(event: NPCDamageByEntityEvent) {
        if (::currentNPC.isInitialized && event.npc == currentNPC) {
            fun handle(player: Player) {
                if (player.info()?.isInBuilderMode == true) {
                    event.isCancelled = true
                    player.error(getLang(player, "boss.error.building"))
                } else {
                    val damage = damageMap.getOrDefault(player, 0.0) + event.damage
                    damageMap[player] = damage
                    totalDamage += event.damage.toFloat()
                }
            }

            if (event.damager is Player) {
                handle(event.damager as Player)
            } else if (event.damager is Projectile) {
                val projectile = event.damager as Projectile
                if (projectile.shooter is Player) {
                    handle(projectile.shooter as Player)
                }
            }
        }
    }

    @EventHandler
    fun onBossDeath(event: NPCDeathEvent) {
        if (event.npc.data().get<Boolean?>("little") == true) {
            event.npc.destroy()
        } else if (::currentNPC.isInitialized && event.npc == currentNPC) {
            if (Base.trueByPercentages(percentageToDropWeapon()))
                event.drops.add(getWeaponForCurrent(0.5))
            if (!withOutEqu)
                Equipments.values().forEach { equipment ->
                    if (Base.trueByPercentages(percentageToDropEqui()))
                        event.drops.add(
                            event.npc.getOrAddTrait(Equipment::class.java)[equipment.index].also {
                                (it.itemMeta as Damageable).damage =
                                    (Material.DIAMOND_CHESTPLATE.maxDurability * random.nextDouble(
                                        0.1,
                                        1.0
                                    )).roundToInt()
                                it.updateItemMeta<ItemMeta> {
                                    val targetEnch = Enchantment.PROTECTION_ENVIRONMENTAL
                                    val oldLvl = getEnchantLevel(targetEnch)
                                    removeEnchant(targetEnch)
                                    addEnchant(targetEnch, (oldLvl * 0.05).roundToInt(), true)
                                }
                            }
                        )
                }
            event.droppedExp = expForCurrent().roundToInt()
            currentNPC.storedLocation.chunk.isForceLoaded = false
            isCurrentBossAlive = false

            Bukkit.getScheduler().runTaskAsynchronously(mPlugin) { _ ->
                val totalCurrency = currencyForCurrent().roundToLong()
                val nextDate = ServerBoss.format.format(ServerBoss.nextDate)
                damageMap.forEach { (p, d) ->
                    val weight = d / totalDamage
                    val currency = (totalCurrency * weight).roundToLong()
                    val getter = p.getter()
                    p.success(getter["boss.award", d, weight, totalCurrency, currency])
                    val info = OfflineInfo.findByUUID(p.uniqueId)
                    if (info == null) {
                        p.error(getter["player.error.unknown"])
                    } else {
                        info.currency += currency
                    }

                    p.info(getter["boss.next", nextDate])
                }
                damageMap.clear()
                totalDamage = 0F
                Bukkit.getScheduler().callSyncMethod(mPlugin) {
                    currentNPC.destroy()
                }
            }
        }
    }
}

fun LivingEntity.healthRate(): Double =
    (health / getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue).let {
        if (it > 1.0) 1.0
        else it
    }

fun NPC.onSpawn(l: () -> Unit) {
    NPCController.spawnListeners[this] = l
}