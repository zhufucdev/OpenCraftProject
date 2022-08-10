package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class ChartUI(plugin: Plugin, info: Info, override val parentInventory: ClickableInventory?) :
    PageInventory<ChartUI.Adapter>(
        plugin = plugin,
        adapter = Adapter(info),
        title = info.getter()["ui.chart.title"].toInfoMessage(),
        itemsOnePage = 36
    ), Backable {
    class Adapter(val info: Info) : PageInventory.Adapter() {
        var isDaily = false
        private var chart = if (isDaily) Game.dailyChart else Game.chart
        override val size: Int
            get() = chart.size
        override val hasToolbar: Boolean
            get() = true

        val getter = info.getter()

        override fun getItem(index: Int, currentPage: Int): ItemStack {
            val player = chart[index]
            return player.skullItem.updateItemMeta<ItemMeta> {
                displayName(
                    if (player == info)
                        getter["ui.chart.self"].toInfoMessage()
                    else
                        player.name?.toInfoMessage() ?: getter["player.unknownName"].toErrorMessage()
                )
                lore(listOf(getter["ui.chart.pos", index + 1].toComponent()))
            }
        }

        override fun getToolbarItem(index: Int): ItemStack {
            return when (index) {
                6 -> Widgets.back.updateItemMeta<ItemMeta> {
                    displayName(getter["ui.back"].toInfoMessage())
                }
                5 -> Widgets.confirm.updateItemMeta<ItemMeta> {
                    displayName(getter["ui.chart.all"].toInfoMessage())
                    if (isDaily) lore(listOf(getter["ui.chart.click"].toTipMessage()))
                    else {
                        addEnchant(Enchantment.ARROW_INFINITE, 1, true)
                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                }
                4 -> Widgets.confirm.updateItemMeta<ItemMeta> {
                    displayName(getter["ui.chart.daily"].toInfoMessage())
                    if (!isDaily) lore(listOf(getter["ui.chart.click"].toTipMessage()))
                    else {
                        addEnchant(Enchantment.ARROW_INFINITE, 1, true)
                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                }
                else -> super.getToolbarItem(index)
            }
        }

        fun refreshChart() {
            chart = if (isDaily) Game.dailyChart else Game.chart
        }
    }

    init {
        setOnToolbarItemClickListener { index, _ ->
            when (index) {
                6 -> back(info.player)
                5 -> {
                    adapter.apply {
                        if (isDaily) {
                            isDaily = false
                        }
                        refreshChart()
                    }
                    refresh()
                }
                4 -> {
                    adapter.apply {
                        if (!isDaily) {
                            isDaily = true
                        }
                        refreshChart()
                    }
                    refresh()
                }
            }
        }
    }
}