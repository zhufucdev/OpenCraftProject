package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import com.zhufu.opencraft.special_item.Coin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class BankInventory(plugin: Plugin, private val info: Info) :
    KeypadInventory(plugin, info.getter(), info.getter()["bank.title"]) {
    private val player get() = info.player

    private var deposit: Boolean
    init {
        inventory.setItem(
            0,
            ItemStack(Material.PAPER).updateItemMeta<ItemMeta> { setDisplayName(getter["bank.tip"].toTipMessage()) })
        deposit = true
        updateMode()
    }

    private fun updateMode() {
        inventory.setItem(27, ItemStack(if (deposit) Material.CHEST else Material.DROPPER).updateItemMeta<ItemMeta> {
            val modeName = "bank.mode.${if (deposit) "deposit" else "withdraw"}"
            setDisplayName(getter["$modeName.title"].toInfoMessage())
            lore = listOf(getter["$modeName.subtitle"], getter["bank.mode.tip"].toTipMessage())
        })
    }

    fun show() {
        show(info.player)
    }

    override fun onConfirm() {
        if (deposit) {
            if (player.setInventory(Coin(1, getter), -amount)) {
                info.currency += amount
                player.success(getter["bank.mode.deposit.done", amount])
            } else {
                player.error(getter["trade.error.lackOfItem"])
            }
        } else {
            if (info.currency >= amount) {
                player.setInventory(Coin(1, getter), amount)
                info.currency -= amount
                player.success(getter["bank.mode.withdraw.done", amount])
            } else {
                player.error(getter["trade.error.poor"])
            }
        }
        close()
    }

    override fun onKeyDown(index: Int) {
        if (amount > Int.MAX_VALUE / 10 || amount < 0) {
            close()
            player.error(getter["bank.error.outOfBound", Int.MAX_VALUE / 10])
        }
    }

    override fun onClick(index: Int) {
        if (index == 27) {
            deposit = !deposit
            updateMode()
        }
    }
}