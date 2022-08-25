package com.zhufu.opencraft

import org.bukkit.Bukkit
import org.bukkit.entity.HumanEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

abstract class IntractableInventory(val plugin: Plugin) : Listener {
    abstract val inventory: Inventory
    protected var isShowing = false
    protected var showingTo: HumanEntity? = null

    protected fun builder(action: InventoryBuilder.() -> Unit) {
        InventoryBuilder(inventory).apply(action)
    }

    fun show(player: HumanEntity) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        player.openInventory(inventory)
        this.showingTo = player
        isShowing = true
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory == this.inventory) {
            event.isCancelled = true
            onClick(event)
        }
    }

    abstract fun onClick(event: InventoryClickEvent)

    fun close() {
        showingTo?.closeInventory()
        HandlerList.unregisterAll(this)
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory == this.inventory) {
            onClose(event.player)
            isShowing = false
            HandlerList.unregisterAll(this)
        }
    }

    open fun onClose(player: HumanEntity) {}
    open fun onMoveItem(event: InventoryDragEvent) {}
}

class InventoryBuilder internal constructor(private val target: Inventory) {
    fun set(x: Int, y: Int, itemStack: ItemStack?) {
        target.setItem(x + y * 9, itemStack)
    }

    fun fill(xRange: IntRange, yRange: IntRange, itemStack: ItemStack?) {
        for (x in xRange) {
            for (y in yRange) {
                target.setItem(x + y * 9, itemStack)
            }
        }
    }
}