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

class BankInventory(plugin: Plugin, private val info: Info) : ClickableInventory(plugin) {
    private val getter = info.getter()
    override val inventory: Inventory = Bukkit.createInventory(null, 36, getter["bank.title"].toInfoMessage())
    private var amount: Int
    private var deposit: Boolean

    init {
        inventory.apply {
            setItem(
                0,
                ItemStack(Material.PAPER).updateItemMeta<ItemMeta> { setDisplayName(getter["bank.tip"].toTipMessage()) })
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
        deposit = true
        amount = 0
        updateMode()
        updateAmount()
    }

    private fun updateMode() {
        inventory.setItem(27, ItemStack(if (deposit) Material.CHEST else Material.DROPPER).updateItemMeta<ItemMeta> {
            val modeName = "bank.mode.${if (deposit) "deposit" else "withdraw"}"
            setDisplayName(getter["$modeName.title"].toInfoMessage())
            lore = listOf(getter["$modeName.subtitle"], getter["bank.mode.tip"].toTipMessage())
        })
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

    fun show() {
        show(info.player)
    }

    override fun onClick(event: InventoryClickEvent) {
        if (event.clickedInventory == inventory) {
            val player = info.player
            when (event.rawSlot) {
                //Key Pad
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

                27 -> {
                    deposit = !deposit
                    updateMode()
                    return
                }
                35 -> {
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
                    return
                }
            }
            if (amount > Int.MAX_VALUE / 10 || amount < 0) {
                close()
                player.error(getter["bank.error.outOfBound", Int.MAX_VALUE / 10])
            } else {
                updateAmount()
            }
        }
    }
}