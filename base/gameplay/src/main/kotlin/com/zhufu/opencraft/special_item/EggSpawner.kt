package com.zhufu.opencraft.special_item

import com.destroystokyo.paper.event.entity.ThrownEggHatchEvent
import com.zhufu.opencraft.*
import com.zhufu.opencraft.npc.AutoDestroy
import com.zhufu.opencraft.npc.NPCHelper
import com.zhufu.opencraft.special_item.dynamic.BindItem
import com.zhufu.opencraft.special_item.dynamic.locate.MemoryLocation
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Equipment
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerEggThrowEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.metadata.LazyMetadataValue
import java.text.DecimalFormat
import java.util.function.Consumer
import kotlin.math.roundToInt

class EggSpawner : BindItem(), Upgradable {
    override val material: Material
        get() = Material.EGG
    var spawning: EntityType = EntityType.ZOMBIE
        private set
    val spawnable: List<EntityType>
        get() {
            val result = mutableListOf(EntityType.ZOMBIE, EntityType.CREEPER)
            if (level >= 2) {
                result.add(EntityType.SPIDER)
                result.add(EntityType.BLAZE)
            }
            if (level >= 3)
                result.add(EntityType.ENDERMAN)
            if (level >= 4)
                result.add(EntityType.IRON_GOLEM)
            return result
        }

    override fun onCreate(owner: Player, vararg args: Any) {
        super.onCreate(owner, *args)
        val getter = owner.getter()
        itemLocation.itemStack.apply {
            amount = 16
            updateItemMeta<ItemMeta> {
                setDisplayName(getter["rpg.egg.name"].toInfoMessage())
                lore = TextUtil.formatLore(getter["rpg.egg.subtitle"]).map { it.toTipMessage() }
                    .plus(
                        TextUtil.formatLore(getter["rpg.egg.content", getter["rpg.egg.${spawning.name.toLowerCase()}"]])
                            .map { it.toInfoMessage() }
                    )
            }
        }
    }

    private var mLevel = 1
    override var level: Int
        get() = mLevel
        set(value) {
            mLevel = value
            updateDisplay()
        }
    override val maxLevel: Int
        get() = 4

    override fun updateDisplay() {
        itemStack.updateItemMeta<ItemMeta> {
            val getter = Language.LangGetter(owner)
            lore = TextUtil.formatLore(getter["rpg.egg.subtitle"]).map { it.toTipMessage() }
                .plus(TextUtil.formatLore(getter["rpg.level", level]).map { it.toInfoMessage() })
                .plus(
                    TextUtil.formatLore(getter["rpg.egg.content", getter["rpg.egg.${spawning.name.toLowerCase()}"]])
                        .map { it.toInfoMessage() }
                )
        }
        itemLocation.push()
    }

    override fun exp(level: Int): Int {
        if (level <= 0 || level > 4) throw IllegalArgumentException("level must be between 1 and 4.")
        return when (level) {
            1 -> 0
            2 -> 400
            3 -> 800
            else -> 1400
        }
    }

    private var isSwitching = false
    private val mListener = object : Listener {
        private fun close() {
            HandlerList.unregisterAll(this)
            isSwitching = false

            Bukkit.getScheduler().runTask(Base.pluginCore) { _ ->
                owner.onlinePlayerInfo!!.player.inventory.apply {
                    saved.forEachIndexed { index, itemStack ->
                        setItem(index, itemStack)
                    }
                }
                saved.clear()
                updateDisplay()
            }
        }

        private fun click(slot: Int): Boolean {
            val available = spawnable
            return when {
                slot < available.size -> {
                    spawning = available[slot]
                    close()
                    true
                }
                slot == 8 -> {
                    close()
                    true
                }
                slot in 0 until 9 -> true
                else -> false
            }
        }

        @EventHandler
        fun onPlayerInteract(event: PlayerInteractEvent) {
            if (event.player != owner.onlinePlayerInfo?.player ?: return) return
            if (event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) {
                event.isCancelled = true
                return
            }
            event.isCancelled = click(event.player.inventory.heldItemSlot)
        }

        @EventHandler
        fun onInventoryClick(event: InventoryClickEvent) {
            if (event.whoClicked.uniqueId != owner.uuid ?: return) return
            if (event.clickedInventory == event.whoClicked.inventory) {
                event.isCancelled = click(event.rawSlot)
            }
        }

        @EventHandler
        fun onPlayerDropItem(event: PlayerDropItemEvent) {
            if (event.player != owner.onlinePlayerInfo?.player ?: return) return
            event.isCancelled = click(event.player.inventory.heldItemSlot)
        }
    }

    private val spawned = arrayListOf<NPC>()
    private val time = hashMapOf<EntityType, Long>()
    private fun spawn(location: Location, type: EntityType) {
        val npc =
            CitizensAPI.getNamedNPCRegistry("temp")
                .createNPC(type, Language[owner.userLanguage, "rpg.egg.puppet"].toInfoMessage())
        npc.apply {
            isProtected = false
            data()["owner"] = owner.uuid
            addTrait(Equipment().apply {
                equipment[1] = ItemStack(Material.IRON_HELMET)
            })
            addTrait(AutoDestroy().apply { delay = existTime(type).toLong() })
            defaultGoalController.addGoal(NPCHelper.createSummonAI(npc, Base.pluginCore), 1)
            spawn(location)
        }
        spawned.add(npc)
    }

    fun cooldown(type: EntityType): Int {
        if (level !in 1..4) error("level must be between 1 and 4.")
        return when (level) {
            1 -> if (type == EntityType.ZOMBIE) 5 else 6
            2 -> when (type) {
                EntityType.ZOMBIE -> 5
                else -> 6
            }
            3 -> when (type) {
                EntityType.ZOMBIE -> 4
                EntityType.ENDERMAN -> 12
                else -> 5
            }
            else -> when (type) {
                EntityType.ZOMBIE, EntityType.SPIDER -> 3
                EntityType.CREEPER, EntityType.BLAZE -> 5
                EntityType.ENDERMAN -> 11
                else -> 30
            }
        } * 20
    }

    fun existTime(type: EntityType): Int {
        val r = cooldown(type) * when (level) {
            1 -> 10.0
            2 -> 8.0
            3 -> 2.0
            else -> 1.5
        }
        return r.roundToInt() bigger 300
    }

    override fun onShutdown() {
        super.onShutdown()
        spawned.forEach { it.despawn(); it.destroy() }
    }

    override fun onPostInit() {
        super.onPostInit()
        listenOther(PlayerEggThrowEvent::class.java, Consumer {
            if (it.player != owner.onlinePlayerInfo?.player ?: return@Consumer) return@Consumer
            if (getSIID(it.egg.item) != SIID) return@Consumer
            // Infinite egg throw
            itemStack.amount = 16
            itemLocation.push()

            synchronized(spawning) {
                val remaining = cooldown(spawning) * 50 - System.currentTimeMillis() + (time[spawning] ?: 0L)
                if (remaining > 0) {
                    it.player.sendActionBar(
                        it.player.getter()[
                                "rpg.egg.cooldown",
                                DecimalFormat("#.##").format(remaining / 1000F)
                        ].toErrorMessage()
                    )
                    it.player.playSound(it.player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1F, 1F)
                    it.isHatching = false
                    return@Consumer
                }
            }
            it.hatchingType = spawning
            it.isHatching = true
            it.numHatches = 1
            it.egg.setMetadata("from", LazyMetadataValue(Base.pluginCore) { it.player.uniqueId })
            // Cool down
            time[spawning] = System.currentTimeMillis()
        })
        listenOther(ThrownEggHatchEvent::class.java, Consumer {
            if (!it.egg.hasMetadata("from")) return@Consumer
            if (it.egg.hasMetadata("spawned")) return@Consumer
            it.egg.setMetadata("spawned", FixedMetadataValue(Base.pluginCore, true))
            it.isHatching = false
            spawn(it.egg.location, it.hatchingType)
        })
    }

    private val saved = arrayListOf<ItemStack?>()
    private var lastClick = 0L
    override fun onInteractWith(event: PlayerInteractEvent) {
        if (event.player != owner.onlinePlayerInfo?.player ?: return) return
        if (System.currentTimeMillis() - lastClick <= 400) return
        lastClick = System.currentTimeMillis()

        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            event.isCancelled = false
            return
        }
        if (!isSwitching) {
            if (getSIID(event.item ?: return) != SIID) return
            val getter = event.player.getter()
            fun void() =
                ItemStack(Material.STRUCTURE_VOID).updateItemMeta<ItemMeta> { setDisplayName(getter["rpg.void"]) }
            event.player.inventory.apply {
                val available = spawnable
                for (i in 0..7) {
                    saved.add(i, getItem(i))
                    setItem(
                        i,
                        if (i < available.size) {
                            val item = available[i]
                            ItemStack(
                                if (item != EntityType.IRON_GOLEM)
                                    Material.getMaterial("${item.name}_SPAWN_EGG")!!
                                else
                                    Material.PUMPKIN
                            ).updateItemMeta<ItemMeta> {
                                setDisplayName(getter["rpg.egg.spawn", getter["rpg.egg.${available[i].name.toLowerCase()}"]].toInfoMessage())
                            }
                        } else
                            void()
                    )
                }
                saved.add(getItem(8))
                setItem(8,
                    ItemStack(Material.BARRIER).updateItemMeta<ItemMeta> {
                        setDisplayName(getter["ui.close"].toInfoMessage())
                    })
            }

            Bukkit.getPluginManager().registerEvents(mListener, Base.pluginCore)
            isSwitching = true
        }
    }

    override fun onSaveStatus(config: ConfigurationSection) {
        if (!isSwitching && owner.onlinePlayerInfo?.let { !NPCHelper.controlling(it) } == true)
            super.onSaveStatus(config)
        else {
            config.set("owner", owner.uuid.toString())
            config.set("location", MemoryLocation(itemLocation.itemStack))
        }
        config.set("lvl", level)
        config.set("selection", spawning.name.toLowerCase())
    }

    override fun onRestore(savedStatus: ConfigurationSection) {
        super.onRestore(savedStatus)
        level = savedStatus.getInt("lvl", 1)
        spawning = EntityType.valueOf(savedStatus.getString("selection")!!.toUpperCase())
    }

    override fun toString(): String = "EggSpawner{ owner = ${owner.name}, " +
            "switching = $isSwitching, " +
            "selected = ${spawning.name}, " +
            "lvl = $level" +
            "}"
}