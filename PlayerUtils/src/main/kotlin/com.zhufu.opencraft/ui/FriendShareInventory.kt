package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.player_community.FriendWrap
import com.zhufu.opencraft.util.toComponent
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toTipMessage
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class FriendShareInventory(info: Info, friend: FriendWrap, plugin: Plugin, override val parentInventory: ClickableInventory) :
    PageInventory<FriendShareInventory.Adapter>(
        title = info.getter()["ui.friend.share.title"].toInfoMessage(),
        adapter = Adapter(info, friend),
        itemsOnePage = 36,
        plugin = plugin
    ), Backable {
    class Adapter(val info: Info, val friend: FriendWrap) : PageInventory.Adapter() {
        override val size: Int
            get() = info.checkpoints.size + 1
        override val hasToolbar: Boolean
            get() = true
        val getter = info.getter()

        override fun getItem(index: Int, currentPage: Int): ItemStack {
            return if (index < info.checkpoints.size) {
                val point = info.checkpoints[index]
                ItemStack(Material.ENDER_PEARL).updateItemMeta<ItemMeta> {
                    displayName(point.name.toInfoMessage())
                    val contains = friend.sharedCheckpoints.contains(point)
                    lore(listOf(getter["ui.friend.point.title"].toComponent(), getter["ui.friend.share." +
                            if (!contains) "start" else "stop"].toTipMessage()))
                    if (contains) {
                        addEnchant(Enchantment.ARROW_INFINITE, 1, true)
                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                }
            } else {
                info.skullItem.updateItemMeta<ItemMeta> {
                    displayName(info.name?.toComponent())
                    lore(listOf(getter["ui.friend.location.title"].toComponent(), getter["ui.friend.share." +
                            if (!friend.shareLocation) "start" else "stop"].toTipMessage()))
                    if (friend.shareLocation) {
                        addEnchant(Enchantment.ARROW_INFINITE, 1, true)
                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                }
            }
        }

        override fun getToolbarItem(index: Int): ItemStack {
            return if (index == 6) {
                Widgets.back.updateItemMeta<ItemMeta> {
                    displayName(getter["ui.back"].toInfoMessage())
                }
            } else {
                super.getToolbarItem(index)
            }
        }
    }

    init {
        setOnItemClickListener { index, _ ->
            if (index < info.checkpoints.size) {
                val point = info.checkpoints[index]
                friend.sharedCheckpoints.apply {
                    if (contains(point)) {
                        remove(point)
                    } else {
                        add(point)
                    }
                }
                refresh()
            } else {
                friend.shareLocation = !friend.shareLocation
                refresh()
            }
        }
        setOnToolbarItemClickListener { index, _ ->
            if (index == 6) back(info.player)
        }
    }
}