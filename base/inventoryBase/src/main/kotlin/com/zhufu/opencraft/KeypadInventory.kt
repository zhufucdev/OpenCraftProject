package com.zhufu.opencraft

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

abstract class KeypadInventory(plugin: Plugin, protected val getter: Language.LangGetter, title: String) :
    ClickableInventory(plugin) {
    final override val inventory: Inventory = Bukkit.createInventory(null, 36, title)
    var amount: Int

    init {
        inventory.apply {
            fun num(n: Int) = ItemStack(Material.BLACK_STAINED_GLASS_PANE).updateItemMeta<ItemMeta> {
                setDisplayName(n.toString())
            }
            setItem(6, num(7))
            setItem(7, num(8))
            setItem(8, num(9))
            setItem(15, num(4))
            setItem(16, num(5))
            setItem(17, num(6))
            setItem(24, num(1))
            setItem(25, num(2))
            setItem(26, num(3))
            setItem(34, num(0))
            setItem(33, Widgets.back.updateItemMeta<ItemMeta> { setDisplayName(getter["ui.delete"].toInfoMessage()) })
            setItem(
                35,
                Widgets.confirm.updateItemMeta<ItemMeta> { setDisplayName(getter["ui.confirm"].toInfoMessage()) }
            )
        }
        amount = 0
        updateAmount()
    }

    private fun updateAmount() {
        fun set(index: Int) =
            inventory.setItem(index, ItemStack(Material.GREEN_STAINED_GLASS_PANE).updateItemMeta<ItemMeta> {
                setDisplayName(amount.toString().toSuccessMessage())
            })
        set(10)
        set(11)
        set(19)
        set(20)
    }

    final override fun onClick(event: InventoryClickEvent) {
        if (event.clickedInventory == inventory) {
            when (event.rawSlot) {
                6 -> amount = amount * 10 + 7
                7 -> amount = amount * 10 + 8
                8 -> amount = amount * 10 + 9
                15 -> amount = amount * 10 + 4
                16 -> amount = amount * 10 + 5
                17 -> amount = amount * 10 + 6
                24 -> amount = amount * 10 + 1
                25 -> amount = amount * 10 + 2
                26 -> amount = amount * 10 + 3
                34 -> amount *= 10
                33 -> amount /= 10
                35 -> onConfirm()
                else -> {
                    onClick(event.rawSlot)
                    return
                }
            }
            onKeyDown(event.rawSlot)
            updateAmount()
        }
    }

    abstract fun onConfirm()
    open fun onClick(index: Int) {}
    open fun onKeyDown(index: Int) {}
}