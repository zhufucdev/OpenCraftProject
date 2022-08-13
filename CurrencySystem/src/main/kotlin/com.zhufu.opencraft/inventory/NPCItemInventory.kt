package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.info
import com.zhufu.opencraft.util.toErrorMessage
import net.citizensnpcs.api.event.NPCRightClickEvent
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Equipment
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.HumanEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import kotlin.collections.ArrayList

abstract class NPCItemInventory(
    var baseLocation: Location,
    private val faceLocation: Location? = null,
    private val item: ItemStack,
    val plugin: JavaPlugin
) : Listener, NPCExistence {
    companion object {
        var id = 314
        val npcList = ArrayList<NPC>()
    }

    val id: Int = ++Companion.id
    abstract val clickableNPC: NPC
    abstract var inventory: Inventory
    abstract var inventoryName: String
    private var isOpened = false

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        Bukkit.getScheduler().runTask(plugin, Runnable {
            initNPC()
        })
    }

    private fun initNPC() {
        baseLocation = baseLocation.block.location

        //Clean the old one
        npcList.firstOrNull { it.id == id }?.destroy()

        clickableNPC.getOrAddTrait(Equipment::class.java)
            .set(Equipment.EquipmentSlot.HAND, item)
        clickableNPC.spawn(baseLocation.clone().add(Vector(0.5, 0.0, 0.5)).apply {
            if (faceLocation != null) yaw = faceLocation.yaw - 180
        })
        npcList.add(clickableNPC)

        Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
            while (!clickableNPC.isSpawned) {
                Thread.sleep(200)
            }
            if (faceLocation != null) clickableNPC.faceLocation(faceLocation)
        }
    }

    open fun cancel(player: HumanEntity?) {
        inventory.clear()
        player?.closeInventory()
        clickableNPC.destroy()
        HandlerList.unregisterAll(this)
    }

    override fun destroy() {
        cancel(null)
    }

    @EventHandler
    fun onNPCClick(event: NPCRightClickEvent) {
        if (event.npc == clickableNPC) {
            if (event.clicker.info()?.isInBuilderMode == true) {
                event.clicker.sendMessage("抱歉，你不能在此时购买或更改物品".toErrorMessage())
                return
            }
            if (isOpened) {
                event.clicker.sendMessage("该物品已经被其他玩家占用".toErrorMessage())
                return
            }
            onInventoryOpen(event.clicker)
            event.clicker.openInventory(inventory)
            isOpened = true
        }
    }

    @EventHandler
    fun onPlayerClickItem(event: InventoryClickEvent) {
        if (event.inventory == inventory) {
            event.isCancelled = onItemClick(event)
        }
    }

    @EventHandler
    fun onPlayerBreakBlock(event: BlockBreakEvent) {
        if (!event.isCancelled && event.block.location == baseLocation) {
            event.isDropItems = false
            this.cancel(event.player)
        }
    }

    @EventHandler
    fun onPlayerCloseInventory(event: InventoryCloseEvent) {
        if (validateInventory(event.inventory)) {
            isOpened = false
            onInventoryClose(event.player)
        }
    }

    fun validateInventory(inventory: Inventory): Boolean = inventory == this.inventory && inventory.location == null

    abstract fun onItemClick(event: InventoryClickEvent): Boolean
    open fun onInventoryOpen(player: HumanEntity) {}
    open fun onInventoryClose(player: HumanEntity) {}
}