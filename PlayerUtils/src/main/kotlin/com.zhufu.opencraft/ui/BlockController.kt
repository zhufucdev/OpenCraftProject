package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

class BlockController(
        plugin: Plugin,
        override val parentInventory: ClickableInventory,
        private val info: BlockLockManager.BlockInfo,
        item: ItemStack,
        private val player: Player
) : ClickableInventory(plugin),Backable {

    private val getter = getLangGetter(player.info())
    override val inventory: Inventory = Bukkit.createInventory(null,9,TextUtil.info(info.name))
    init {
        inventory.apply {
            setItem(0, item)
            setItem(
                    2,
                    Widgets.rename.apply {
                        itemMeta = itemMeta!!.apply {
                            setDisplayName(TextUtil.info(getter["ui.block.selecting.title"]))
                            val newLore = ArrayList<String>()
                            newLore.add(TextUtil.getColoredText(getter["ui.block.selecting.tip.1"],TextUtil.TextColor.AQUA))
                            for (i in 2 .. 6){
                                newLore.add(TextUtil.tip(getter["ui.block.selecting.tip.$i"]))
                            }
                            lore = newLore
                        }
                    }
            )
            setItem(
                    4,
                    Widgets.cancel.apply {
                        itemMeta = itemMeta!!.apply {
                            setDisplayName(TextUtil.getColoredText(getter["ui.delete"],TextUtil.TextColor.RED))
                            val newLore = ArrayList<String>()
                            TextUtil.formatLore(getter["ui.block.deleteAlert"]).forEach {
                                newLore.add(TextUtil.warn(it))
                            }
                            lore = newLore
                        }
                    }
            )
            if (info.parent != null)
                setItem(
                        6,
                        ItemStack(Material.FISHING_ROD).apply {
                            itemMeta = itemMeta!!.apply {
                                setDisplayName(TextUtil.tip(getter["ui.block.ungroup"]))
                            }
                        }
                )
            setItem(
                    8,
                    Widgets.back.apply {
                        itemMeta = itemMeta!!.apply {
                            setDisplayName(TextUtil.tip(getter["ui.back"]))
                        }
                    }
            )
        }
    }
    override fun onClick(event: InventoryClickEvent) {
        when (event.rawSlot){
            2 -> {
                BlockLockManager.selected[player] = info
                player.info(getter["ui.block.selecting.done",info.name])
                close()
            }
            4 -> {
                BlockLockManager.remove(info)
                player.success(getter["block.delete",info.name,getter["block.block"]])
                if (parentInventory is PageInventory<*>) parentInventory.refresh()
                back(player)
            }
            6 -> {
                info.parent?.remove(info)
                BlockLockManager.add(info)
                player.success(getter["command.done"])
                if (parentInventory is PageInventory<*>) {
                    parentInventory.refresh()
                    if (parentInventory is Backable){
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