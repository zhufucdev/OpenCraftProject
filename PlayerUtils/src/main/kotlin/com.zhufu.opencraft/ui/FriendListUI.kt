package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.player_community.MessagePool
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class FriendListUI(info: Info, plugin: Plugin, override val parentInventory: ClickableInventory) :
    PageInventory<FriendListUI.Adapter>(
        title = getLang(info, "ui.friend.title").toInfoMessage(),
        adapter = Adapter(info),
        itemsOnePage = 36,
        plugin = plugin
    ), Backable {
    private val friendship get() = adapter.friendship

    class Adapter(val info: Info) : PageInventory.Adapter() {
        val getter = info.getter()
        val friendship by lazy { info.friendship }
        override val size: Int
            get() = if (!addMode) info.friendship.size + 1 else strangers.size
        override val hasToolbar: Boolean
            get() = true

        val strangers by lazy {
            val r = arrayListOf<ServerPlayer>()
            ServerPlayer.forEachSaved {
                if (it.name != null && it != info && !friendship.contains(it))
                    r.add(it)
            }
            r
        }

        var addMode = false

        override fun getItem(index: Int, currentPage: Int): ItemStack {
            return if (!addMode) {
                if (index < friendship.size) {
                    val friend = friendship[index]
                    friend.friend.skullItem.updateItemMeta<ItemMeta> {
                        setDisplayName(friend.name)
                        lore = arrayListOf<String>().apply {
                            if (friend.isFriend)
                                friend.statics(getter).forEach {
                                    add(it.toInfoMessage())
                                }
                            add(getter["ui.friend.click"].toTipMessage())
                        }
                    }
                } else {
                    Widgets.confirm.updateItemMeta<ItemMeta> {
                        setDisplayName(getter["ui.friend.add"].toInfoMessage())
                    }
                }
            } else {
                val player = strangers[index]
                player.skullItem.updateItemMeta<ItemMeta> {
                    setDisplayName(player.name)
                    lore = listOf(
                        getter["ui.friend.send"].toTipMessage(),
                        getter["ui.friend.count", player.friendship.size]
                    )
                }
            }
        }

        override fun getToolbarItem(index: Int): ItemStack {
            return if (index == 6) {
                Widgets.back.updateItemMeta<ItemMeta> {
                    setDisplayName(getter["ui.back"].toInfoMessage())
                }
            } else {
                super.getToolbarItem(index)
            }
        }
    }

    init {
        setOnItemClickListener { index, _ ->
            if (!adapter.addMode) {
                if (index < friendship.size) {
                    val friend = friendship[index]
                    close()
                    FriendInteractUI(plugin, friend, info, this).show()
                } else {
                    adapter.addMode = true
                    close()
                    info.player.sendActionText(adapter.getter["ui.friend.booting"].toInfoMessage())
                    Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
                        Bukkit.getScheduler().callSyncMethod(plugin) {
                            show(info.player)
                        }
                        refresh()
                    }
                }
            } else {
                val player = adapter.strangers[index]
                friendship.add(player)
                info.player.success(adapter.getter["user.friend.sent"])
                player.messagePool.apply {
                    val a = add(
                        text = "\$info\${user.friend.request.title,${info.name}}",
                        type = MessagePool.Type.System
                    )
                    val b = add(
                        text = "\$tip\${user.friend.request.tip,${info.name}," +
                                "${info.name}}",
                        type = MessagePool.Type.System
                    )
                    a.recordTime()
                    if (player.isOnline) {
                        with(player.onlinePlayerInfo!!) {
                            sendTo(this, a)
                            sendTo(this, b)
                        }
                    }
                }
                back(info.player)
            }
        }
        setOnToolbarItemClickListener { index, _ ->
            if (index == 6) {
                if (!adapter.addMode)
                    back(info.player)
                else {
                    adapter.addMode = false
                    refresh()
                }
            }
        }
    }
}