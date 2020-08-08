package com.zhufu.opencraft

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

abstract class DraggableInventory(val plugin: Plugin, row: Int, title: String) : Listener {
    protected val inventory = Bukkit.createInventory(null, row * 9, title)
    private val protecting = hashMapOf<Int, Boolean>()
    private val overridability = hashMapOf<Int, Boolean>()
    fun setItem(index: Int, content: ItemStack, protected: Boolean = true, overridable: Boolean = false) {
        inventory.setItem(index, content)
        protecting[index] = protected
        overridability[index] = overridable
        Bukkit.getScheduler().runTask(plugin) { _ ->
            showing?.updateInventory()
        }
    }

    fun setItem(x: Int, y: Int, content: ItemStack, protected: Boolean = true, overridable: Boolean = false) =
        setItem(y * 9 + x, content, protected, overridable)

    fun clear(index: Int) {
        inventory.setItem(index, null)
        protecting.remove(index)
        overridability.remove(index)
        Bukkit.getScheduler().runTask(plugin) { _ ->
            showing?.updateInventory()
        }
    }

    fun clear(x: Int, y: Int) = clear(y * 9 + x)

    var showing: Player? = null
        private set

    fun show(showing: Player) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        showing.openInventory(inventory)
        this.showing = showing
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory != inventory) return
        if (event.clickedInventory == inventory) {
            // In this inventory
            if (protecting[event.rawSlot] == true) {
                event.isCancelled = true
                return
            }
            event.isCancelled = false
            if (!event.isShiftClick) {
                val emptyCursor = event.cursor == null || event.cursor?.type == Material.AIR
                when {
                    !emptyCursor && event.currentItem == null -> onPlace(event)
                    !emptyCursor && overridability[event.rawSlot] == false -> event.isCancelled = true
                    emptyCursor && event.currentItem != null -> onTake(event)
                }
            }
            else
                onTake(event)
        } else if (event.clickedInventory == event.whoClicked.inventory) {
            // In own inventory
            event.isCancelled = false
            if (!event.isShiftClick && event.currentItem != null)
                onPick(event)
            else if (event.cursor != null) {
                onPlace(event)
                if (event.currentItem != null)
                    onPick(event)
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory == inventory) {
            HandlerList.unregisterAll(this)
            onClose()
        }
    }

    open fun onPick(event: InventoryClickEvent) {}

    open fun onPlace(event: InventoryClickEvent) {}

    open fun onTake(event: InventoryClickEvent) {}

    open fun onClose() {}
}