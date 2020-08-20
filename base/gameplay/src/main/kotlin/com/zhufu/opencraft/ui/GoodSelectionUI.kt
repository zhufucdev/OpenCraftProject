package com.zhufu.opencraft.ui

import com.zhufu.opencraft.Language
import com.zhufu.opencraft.PageInventory
import com.zhufu.opencraft.Prise
import com.zhufu.opencraft.toInfoMessage
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

class GoodSelectionUI(goods: Map<ItemStack, Prise<*>>, getter: Language.LangGetter, plugin: Plugin) :
    PageInventory<GoodSelectionUI.Adapter>(
        getter["rpg.ui.goodSelection.title"].toInfoMessage(),
        Adapter(goods, getter),
        36,
        plugin
    ) {
    class Adapter(private val goods: Map<ItemStack, Prise<*>>, private val getter: Language.LangGetter) : PageInventory.Adapter() {
        override val size: Int
            get() = goods.size

        override fun getItem(index: Int, currentPage: Int): ItemStack {
            TODO("Not yet implemented")
        }
    }
}