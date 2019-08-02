package com.zhufu.opencraft

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Shulker
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.DoubleChestInventory
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Suppress("unused")
object BlockListener : Listener {
    val creationMap = HashMap<Player, HashMap<Location, Boolean>>()
    val shulkerMap = HashMap<Location, Shulker>()
    private val coolDown = ArrayList<Player>()

    private lateinit var mPlugin: Plugin
    fun init(plugin: Plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        mPlugin = plugin
    }

    fun cleanUp() {
        creationMap.values.forEach {
            it.keys.forEach { location ->
                location.getNearbyEntitiesByType(Shulker::class.java, 0.0).forEach { entity ->
                    entity.remove()
                }
            }
        }
    }

    @EventHandler
    fun onBreakBlock(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player
        val info = BlockLockManager[block.location.toBlockLocation()] ?: return
        if (!info.canAccess(player)) {
            player.error(getLang(player, "block.error.inaccessible"))
            event.isCancelled = true
        } else if (info.ownedBy(player)) {
            BlockLockManager.remove(info)
            val getter = player.getter()
            player.info(getter["block.delete", info.name, getter["block.block"]])
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockExplode(event: EntityExplodeEvent) {
        if (event.blockList().any { BlockLockManager[it.location] != null }) {
            event.isCancelled = true
        }
    }

    private val Block.isInventoryHolder
        get() = type.let {
            it == Material.CHEST || it == Material.TRAPPED_CHEST
                    || it == Material.FURNACE
                    || it.name.contains("SHULKER_BOX")
                    || it == Material.DISPENSER
                    || it == Material.DROPPER
                    || it == Material.HOPPER
        }

    private fun spawnShulker(location: Location) {
        (location.world.spawnEntity(location.clone().add(0.5, 0.0, 0.5), EntityType.SHULKER) as Shulker).apply {
            setAI(false)
            addPotionEffect(
                PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, 1, false, false)
            )
            isInvulnerable = true
            shulkerMap[location] = this
        }
    }

    private fun turn(shulker: Shulker, selected: Boolean) {
        shulker.isGlowing = selected
    }

    fun cleanFor(player: Player, isCancel: Boolean = false) {
        creationMap[player]?.forEach { t, _ ->
            t.center.getNearbyEntitiesByType(Shulker::class.java, 0.1).forEach {
                it.remove()
            }
        } ?: return
        creationMap.remove(player)
        if (isCancel)
            player.info(player.getter()["block.cancel"])
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val result = creationMap[event.player] ?: return
        val block = event.player.getTargetBlock(7) ?: return
        if (!result.containsKey(block.location) && block.isInventoryHolder && !BlockLockManager.contains(block.location)) {
            val isChest = block.getState(false) is Chest
            val chest = if (isChest) block.getState(false) as Chest else null
            if (isChest && chest!!.inventory is DoubleChestInventory) {
                (chest.inventory as DoubleChestInventory).apply {
                    val l1 = (rightSide.holder as Chest).location
                    val l2 = (leftSide.holder as Chest).location
                    spawnShulker(l1)
                    spawnShulker(l2)
                    result[l1] = false
                    result[l2] = false
                }
            } else {
                spawnShulker(block.location)
                // false means the shulker has spawned and hasn't been confirmed to join the final results.
                result[block.location] = false
            }
        }
    }

    private val Location.center get() = clone().toBlockLocation().add(0.5, 0.0, 0.5)
    private fun doTurn(player: Player, location: Location) {
        val submap = creationMap[player]!!
        val selected = !submap[location]!!
        val block = location.block
        val isChest = block.getState(false) is Chest
        val chest = if (isChest) block.getState(false) as Chest else null
        if (isChest && chest!!.inventory is DoubleChestInventory) {
            (chest.inventory as DoubleChestInventory).apply {
                val l1 = (rightSide.holder as Chest).location
                val l2 = (leftSide.holder as Chest).location
                turn(shulkerMap[l1]!!, selected)
                turn(shulkerMap[l2]!!, selected)
                submap[l1] = selected
                submap[l2] = selected
            }
        } else {
            turn(shulkerMap[location]!!, selected)
            submap[location] = selected
        }
        coolDown.add(player)
        Bukkit.getScheduler().runTaskLater(mPlugin, { _ ->
            coolDown.remove(player)
        }, 3)
    }

    @EventHandler
    fun onPlayerClickShulker(event: PlayerInteractEntityEvent) {
        if (!coolDown.contains(event.player) && creationMap.containsKey(event.player) && event.rightClicked is Shulker) {
            val location = shulkerMap.filter { it.value == event.rightClicked }.keys.firstOrNull()
            if (location != null) {
                doTurn(event.player, location)
            }
        }
    }

    @EventHandler
    fun onPlayerClick(event: PlayerInteractEvent) {
        if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
            cleanFor(event.player, true)
        } else if (event.action == Action.RIGHT_CLICK_BLOCK) {
            if (creationMap.containsKey(event.player)
                && !coolDown.contains(event.player)
                && event.clickedBlock!!.isInventoryHolder
                && creationMap[event.player]!!.containsKey(event.clickedBlock!!.location)
            ) {
                event.isCancelled = true
                doTurn(event.player, event.clickedBlock!!.location)
            }
        }
    }

    @EventHandler
    fun onPlayerLeftClickShulker(event: EntityDamageByEntityEvent) {
        if (event.entity is Shulker && event.damager is Player) {
            cleanFor(event.damager as Player, true)
        }
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val info = BlockLockManager[(event.inventory.location?.toBlockLocation() ?: return)] ?: return
        if (!info.canAccess(event.player.uniqueId)) {
            event.isCancelled = true
            event.player.error(getLang(event.player, "block.error.inaccessible"))
        }
        info.accessMap[Date()] = event.player.uniqueId
    }
}