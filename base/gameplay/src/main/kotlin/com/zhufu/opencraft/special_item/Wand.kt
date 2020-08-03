package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import com.zhufu.opencraft.special_item.MagicBook.Magic.*
import com.zhufu.opencraft.special_item.base.BindItem
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.memory.MemoryKey
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionEffectType.HARM
import org.bukkit.potion.PotionEffectType.HEAL
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.cos

class Wand : BindItem() {
    var level: Int = 1

    override val material: Material
        get() = Material.STICK

    override fun onCreate(owner: Player, vararg args: Any) {
        super.onCreate(owner, *args)
        val getter = owner.getter()
        itemLocation.itemStack.updateItemMeta<ItemMeta> {
            setDisplayName(getter["rpg.wand.name"].toInfoMessage())
            lore = TextUtil.formatLore(getter["rpg.wand.subtitle"]).map { it.toInfoMessage() }
                .plus(listOf(getter["rpg.level", level].toInfoMessage(), getter["rpg.wand.click"].toTipMessage()))
            // Visual
            addEnchant(Enchantment.ARROW_INFINITE, 1, true)
            addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
    }

    fun updateDisplay() {
        val getter = Language.LangGetter(owner)
        itemStack.updateItemMeta<ItemMeta> {
            lore = TextUtil.formatLore(getter["rpg.wand.subtitle"]).map { it.toInfoMessage() }
                .plus(listOf(getter["rpg.level", level].toInfoMessage(), getter["rpg.wand.click"].toTipMessage()))
        }
        itemLocation.push()
    }

    private var activated = false
    private val itemReplaced = arrayListOf<ItemStack?>()
    private val itemExit: ItemStack
        get() = ItemStack(Material.BARRIER).updateItemMeta<ItemMeta> {
            val getter = Language.LangGetter(owner)
            setDisplayName(getter["ui.close"].toInfoMessage())
        }
    private val mListener = object : Listener {
        private fun disable() {
            HandlerList.unregisterAll(this)

            Bukkit.getScheduler().runTask(Base.pluginCore) { _ ->
                // Restore the items
                owner.onlinePlayerInfo!!.player.inventory.apply {
                    for (i in 0 until 9) {
                        setItem(i, itemReplaced[i])
                    }
                    itemReplaced.clear()
                }
            }

            activated = false
            owner.onlinePlayerInfo?.player?.exp = 0F
        }

        @EventHandler
        fun onMagicInteract(event: PlayerInteractEvent) {
            if (event.player != owner.onlinePlayerInfo?.player ?: return) return
            if (event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) return
            if (!activated) return

            event.isCancelled = true

            val slot = event.player.inventory.heldItemSlot
            if (slot < values().size) {
                playMagic(
                    what = values()[slot],
                    who = event.player,
                    direction = event.player.location.direction
                )
                val cost = cost(values()[slot])
                if (owner.mp >= cost) {
                    owner.mp -= cost
                    owner.onlinePlayerInfo?.showMP()
                } else {
                    event.player.sendActionBar(Language.LangGetter(owner)["rpg.wand.error.mp"].toErrorMessage())
                }
            } else if (slot == 8) {
                disable()
            }
        }

        @EventHandler
        fun onInventoryClick(event: InventoryClickEvent) {
            if (event.whoClicked.uniqueId != owner.uuid ?: return) return
            event.isCancelled = true
            disable()
        }

        @EventHandler
        fun onDropItem(event: PlayerDropItemEvent) {
            if (event.player != owner.onlinePlayerInfo?.player ?: return) return
            event.isCancelled = true
            disable()
        }
    }

    override fun onInteractWith(event: PlayerInteractEvent) {
        super.onInteractWith(event)
        if (event.player != owner.onlinePlayerInfo?.player ?: return) return
        if (event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) return

        if (!activated) {
            owner.onlinePlayerInfo?.showMP(false)
            Bukkit.getScheduler().runTask(Base.pluginCore) { _ ->
                event.player.inventory.apply {
                    for (i in 0 until 9) {
                        // Copy the items
                        itemReplaced.add(i, getItem(i))
                        // Replace them with magic indicators
                        setItem(
                            i,
                            values().let { magics ->
                                if (magics.size > i)
                                    magics[i].indicator(event.player.getter())
                                else null
                            }
                        )
                    }
                    setItem(8, itemExit)
                }
            }

            Bukkit.getPluginManager().registerEvents(mListener, Base.pluginCore)
            activated = true

            event.isCancelled = true
        }
    }

    // Rules
    // Range of magic in block
    val range: Int
        get() = when (level) {
            1 -> 7
            2 -> 13
            3 -> 17
            else -> 23
        }
    val radius: Double
        get() = when (level) {
            1 -> 0.2
            2 -> 0.4
            3 -> 0.6
            else -> 1.0
        }

    // Duration of effects in tick
    val effectDuration: Int
        get() = when (level) {
            1 -> 100
            2 -> 200
            else -> 250
        }

    fun effectLevel(kind: MagicBook.Magic): Int {
        if (level == 1) return 1
        return when (kind) {
            POISON -> when (level) {
                in 1..3 -> 1
                else -> 2
            }
            FIRE -> 1
            SLOWNESS -> when (level) {
                2 -> 2
                else -> 3
            }
            WEAKNESS -> when (level) {
                in 2..3 -> 2
                else -> 3
            }
            INSTANT_DAMAGE -> when (level) {
                in 2..3 -> 1
                else -> 2
            }
            HEALING -> when (level) {
                2 -> 1
                else -> 2
            }
            HEALING_POOL -> level - 1
        }
    }


    // Every damage to an entity will cost 0.1 power. When the power
    // goes down to zero, the magic fades.
    val power get() = level * 0.25

    fun playMagic(what: MagicBook.Magic, who: Player, direction: Vector) {
        val l0 = who.eyeLocation.clone()
        val v = direction.unitVector().multiply(0.5)
        var power = this.power

        fun orbit(x: Double, t: Int = 0): Location {
            return if (x != 0.0) {
                val y = ((x - l0.x) * direction.y / direction.x + l0.y).takeIf { it.isFinite() } ?: t * v.y + l0.y
                val z = ((y - l0.y) * direction.z / direction.y + l0.z).takeIf { it.isFinite() } ?: t * v.z + l0.z
                Location(l0.world, x, y, z)
            } else {
                Location(l0.world, x, t * v.y + l0.y, l0.z)
            }
        }

        var task: BukkitTask? = null
        var x = l0.x
        var t = 0
        val affected = arrayListOf<UUID>()
        task = Bukkit.getScheduler().runTaskTimer(Base.pluginCore, Runnable {
            x += v.x
            val location = orbit(x, t)
            if (location.distance(l0) > range || location.block.type.isSolid) {
                task!!.cancel()
                return@Runnable
            }
            l0.world.spawnParticle(Particle.SPELL_WITCH, location, 1)
            location.getNearbyLivingEntities(2.0)
                .forEach {
                    if (it == who || affected.contains(it.uniqueId) ||
                        !orbit(it.location.x, t).toVector().let { o ->
                            it.boundingBox.overlaps(BoundingBox.of(
                                o,
                                it.location.toVector().subtract(o).unitVector().multiply(radius).add(o)
                            ))
                        }
                    )
                        return@forEach

                    if (what == FIRE) {
                        it.fireTicks = effectDuration
                    } else {
                        it.addPotionEffect(
                            PotionEffect(
                                what.potionType.let { t ->
                                    if (it.type.isUndead) {
                                        when (t) {
                                            HARM -> HEAL
                                            HEAL -> HARM
                                            else -> t
                                        }
                                    } else t
                                },
                                effectDuration.takeIf { what.potionType != HARM && what.potionType != HEAL }
                                    ?: 1,
                                effectLevel(what)
                            )
                        )
                        it.setMemory(MemoryKey.ANGRY_AT, who.uniqueId)
                    }
                    affected.add(it.uniqueId)
                    power -= 0.1
                    // Check power
                    if (power <= 0) {
                        task!!.cancel()
                        return@Runnable
                    }
                }
            t++
        }, 0, 1)
    }

    override fun onRestore(savedStatus: ConfigurationSection) {
        super.onRestore(savedStatus)
        level = savedStatus.getInt("lvl", 1)
    }

    override fun onSaveStatus(config: ConfigurationSection) {
        super.onSaveStatus(config)
        config.set("lvl", level)
    }

    companion object {
        fun cost(kind: MagicBook.Magic) = when (kind) {
            POISON -> 15
            FIRE -> 10
            SLOWNESS -> 8
            WEAKNESS -> 15
            INSTANT_DAMAGE -> 10
            HEALING -> 10
            HEALING_POOL -> 12
        }
    }
}