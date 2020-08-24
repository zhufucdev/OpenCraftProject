package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class GoodSelectionUI(goods: List<ItemPrisePair>, getter: Language.LangGetter, plugin: Plugin) :
    PageInventory<GoodSelectionUI.Adapter>(
        getter["rpg.ui.goodSelection.title"].toInfoMessage(),
        Adapter(goods, getter),
        36,
        plugin
    ) {
    class Adapter(private val goods: List<ItemPrisePair>, private val getter: Language.LangGetter) : PageInventory.Adapter() {
        override val size: Int
            get() = goods.size

        override fun getItem(index: Int, currentPage: Int): ItemStack = goods[index].let {
            it.item.clone().updateItemMeta<ItemMeta> {
                lore = ArrayList((lore?: listOf())).apply { add(getter["rpg.ui.goodSelection.tip", it.prise.toString(getter)]) }
            }
        }
    }

    init {
        setOnItemClickListener { index, _ ->
            if (index >= goods.size) return@setOnItemClickListener

            val item = goods[index]
            mSelectListener?.invoke(item.item, item.prise)
            close()
        }
    }

    private var mCloseListener: (() -> Unit)? = null
    fun setCloseListener(l: () -> Unit) {
        mCloseListener = l
    }
    private var mSelectListener: ((ItemStack, Prise<*>) -> Unit)? = null
    fun setSelectListener(l: (ItemStack, Prise<*>) -> Unit) {
        mSelectListener = l
    }

    override fun onClose(player: HumanEntity) {
        mCloseListener?.invoke()
    }
}