package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class BlockController(
    plugin: Plugin,
    override val parentInventory: IntractableInventory,
    private val info: BlockLockManager.BlockInfo,
    item: ItemStack,
    private val player: Player
) : IntractableInventory(plugin), Backable {

    private val getter = getLangGetter(player.info())
    override val inventory: Inventory = Bukkit.createInventory(null, 9, info.name.toInfoMessage())

    init {
        inventory.apply {
            setItem(0, item)
            setItem(
                2,
                Widgets.rename.apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.block.selecting.title"].toInfoMessage())
                        val newLore = ArrayList<Component>()
                        newLore.add(
                            getter["ui.block.selecting.tip.1"].toComponent()
                                .color(NamedTextColor.AQUA)
                        )
                        for (i in 2..6) {
                            newLore.add(getter["ui.block.selecting.tip.$i"].toTipMessage())
                        }
                        lore(newLore)
                    }
                }
            )
            setItem(
                4,
                Widgets.cancel.apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.delete"].toComponent().color(NamedTextColor.RED))
                        lore(
                            TextUtil.formatLore(getter["ui.block.deleteAlert"]).map {
                                it.toWarnMessage()
                            }
                        )
                    }
                }
            )
            if (info.parent != null)
                setItem(
                    6,
                    ItemStack(Material.FISHING_ROD).updateItemMeta<ItemMeta> {
                        displayName(getter["ui.block.ungroup"].toTipMessage())
                    }
                )
            setItem(
                8,
                Widgets.back.updateItemMeta<ItemMeta> {
                    displayName(getter["ui.back"].toTipMessage())
                }
            )
        }
    }

    override fun onClick(event: InventoryClickEvent) {
        when (event.rawSlot) {
            2 -> {
                BlockLockManager.selected[player] = info
                player.info(getter["ui.block.selecting.done", info.name])
                close()
            }

            4 -> {
                BlockLockManager.remove(info)
                player.success(getter["block.delete", info.name, getter["block.block"]])
                if (parentInventory is PageInventory<*>) parentInventory.refresh()
                back(player)
            }

            6 -> {
                info.parent?.remove(info)
                BlockLockManager.add(info)
                player.success(getter["command.done"])
                if (parentInventory is PageInventory<*>) {
                    parentInventory.refresh()
                    if (parentInventory is Backable) {
                        parentInventory.parentInventory.apply {
                            if (this is PageInventory<*>)
                                refresh()
                        }
                    }
                }
                back(player)
            }

            8 -> back(player)
        }
    }
}