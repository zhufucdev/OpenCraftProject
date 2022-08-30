package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.util.*
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin
import java.time.format.DateTimeFormatter

class AdvertisementManageUI(owner: Info, getter: Language.LangGetter, plugin: Plugin,
                            override val parentInventory: IntractableInventory?) :
    PageInventory<AdvertisementManageUI.ADAdapter>(
        getter["ad.title"].toInfoMessage(),
        ADAdapter(owner),
        27,
        plugin
    ), Backable {
    class ADAdapter(val owner: Info) : Adapter() {
        var list = Advertisement.list().filter { it.owner == owner }
        val getter = owner.getter()
        override val size: Int
            get() = list.size + 1

        override fun onRefresh() {
            list = Advertisement.list().filter { it.owner == owner }
        }

        override fun getItem(index: Int, currentPage: Int) =
            if (index < size - 1) {
                ItemStack(Material.ITEM_FRAME).updateItemMeta<ItemMeta> {
                    val ad = list[index]
                    displayName(
                        (ad.name.takeIf { it.isNotEmpty() } ?: getter["ad.unnamed"]).toTitleMessage()
                    )
                    val date = DateTimeFormatter
                        .ofPattern("MM/dd hh:mm B", owner.locale)
                        .format(ad.startTime.atZone(Base.timeZone.toZoneId()))
                    val weight = ad.weigh(list)
                    lore(
                        listOf(
                            getter["ad.date", date].toComponent(),
                            getter["ad.weight", String.format("%.2f", weight * 100)].toComponent()
                        )
                    )
                }
            } else {
                Widgets.confirm.updateItemMeta<ItemMeta> {
                    displayName(getter["ad.rent"].toSuccessMessage())
                    lore(listOf(getter["ad.click"].toTipMessage()))
                }
            }

        override fun getToolbarItem(index: Int): ItemStack =
            if (index == 7) {
                Widgets.back.updateItemMeta<ItemMeta> { displayName(getter["ui.back"].toInfoMessage()) }
            } else {
                super.getToolbarItem(index)
            }

        override val hasToolbar: Boolean
            get() = true
    }

    init {
        setOnItemClickListener { index, _ ->
            if (index < adapter.list.size) {
                val ad = adapter.list[index]
                AdvertisementEditUI(plugin, owner, ad, this).show()
            } else {
                AdvertisementEditUI(plugin, owner, parentInventory = this).show()
            }
        }

        setOnToolbarItemClickListener { index, _ ->
            if (index == 7) {
                back(showingTo!!)
            }
        }
    }

    fun show() = show(adapter.owner.player)
}