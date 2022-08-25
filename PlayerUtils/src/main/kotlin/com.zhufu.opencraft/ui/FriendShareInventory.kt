package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.player_community.Friendship
import com.zhufu.opencraft.util.toComponent
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toTipMessage
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class FriendShareInventory(info: Info, friendship: Friendship, plugin: Plugin, override val parentInventory: IntractableInventory) :
    PageInventory<FriendShareInventory.Adapter>(
        title = info.getter()["ui.friend.share.title"].toInfoMessage(),
        adapter = Adapter(info, friendship),
        itemsOnePage = 36,
        plugin = plugin
    ), Backable {
    class Adapter(val info: Info, val friendship: Friendship) : PageInventory.Adapter() {
        override val size: Int
            get() = checkpoints.size + 1
        var checkpoints = info.checkpoints.sortedBy { it.name }
        override val hasToolbar: Boolean
            get() = true
        val getter = info.getter()

        override fun getItem(index: Int, currentPage: Int): ItemStack {
            return if (index < checkpoints.size) {
                val point = checkpoints[index]
                ItemStack(Material.ENDER_PEARL).updateItemMeta<ItemMeta> {
                    displayName(point.name.toInfoMessage())
                    val contains = friendship.sharedCheckpoints.contains(point)
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
                            if (!friendship.shareLocation) "start" else "stop"].toTipMessage()))
                    if (friendship.shareLocation) {
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

        override fun onRefresh() {
            checkpoints = info.checkpoints.sortedBy { it.name }
        }
    }

    init {
        setOnItemClickListener { index, _ ->
            if (index < adapter.checkpoints.size) {
                val point = adapter.checkpoints[index]
                friendship.sharedCheckpoints.apply {
                    if (contains(point)) {
                        friendship.removeSharedCheckpoint(point)
                    } else {
                        friendship.shareCheckpoint(point)
                    }
                }
                refresh()
            } else {
                friendship.shareLocation = !friendship.shareLocation
                refresh()
            }
        }
        setOnToolbarItemClickListener { index, _ ->
            if (index == 6) back(info.player)
        }
    }
}