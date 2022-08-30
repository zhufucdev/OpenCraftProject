package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.inventory.PayInputDialog
import com.zhufu.opencraft.inventory.PaymentDialog
import com.zhufu.opencraft.player_community.Friendship
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.util.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class FriendInteractUI(
    plugin: Plugin,
    private val friendship: Friendship,
    private val info: Info,
    override val parentInventory: IntractableInventory
) : IntractableInventory(plugin), Backable {
    override val inventory: Inventory = Bukkit.createInventory(null, 36)
    val getter by lazy { Language.LangGetter(friendship.owner) }

    private fun refresh() {
        if (friendship.exits)
            inventory.apply {
                setItem(13, friendship.friend.skullItem.updateItemMeta<ItemMeta> {
                    displayName(friendship.name?.toComponent())
                    if (friendship.isFriend)
                        lore(buildList {
                            friendship.statics(getter).forEach {
                                add(it.toInfoMessage())
                            }
                        })
                })
                val deleteItem = Widgets.cancel.updateItemMeta<ItemMeta> {
                    displayName(getter["ui.friend.del"].toErrorMessage())
                }
                if (friendship.isFriend) {
                    setItem(18, ItemStack(Material.GOLD_INGOT).updateItemMeta<ItemMeta> {
                        displayName(getter["ui.friend.transfer.title"].toInfoMessage())
                        lore(listOf(getter["ui.friend.transfer.click"].toTipMessage()))
                    })
                    setItem(20, ItemStack(Material.OAK_SIGN).updateItemMeta<ItemMeta> {
                        displayName(getter["ui.friend.msg.title"].toInfoMessage())
                        lore(listOf(getter["ui.friend.msg.tip", friendship.name].toTipMessage()))
                    })
                    setItem(22, ItemStack(Material.CHEST).updateItemMeta<ItemMeta> {
                        displayName(getter["ui.friend.inventory.title"].toInfoMessage())
                        lore(listOf(
                            getter["ui.friend.inventory.${if (friendship.sharedInventory == null) "buy" else "check"}"]
                                .toTipMessage()
                        ))
                    })
                    setItem(24, ItemStack(Material.CAKE).updateItemMeta<ItemMeta> {
                        displayName(getter["ui.friend.share.title"].toInfoMessage())
                        lore(listOf(getter["ui.friend.share.click"].toTipMessage()))
                    })
                    setItem(26, deleteItem)
                } else {
                    setItem(18, Widgets.confirm.updateItemMeta<ItemMeta> {
                        displayName(getter["ui.friend.add"].toSuccessMessage())
                    })
                    setItem(20, deleteItem)
                }
                setItem(inventory.size - 1, Widgets.back.updateItemMeta<ItemMeta> {
                    displayName(getter["ui.back"].toInfoMessage())
                })
            }
        else {
            (parentInventory as FriendListUI).refresh()
            back(info.player)
        }
    }

    init {
        refresh()
        info.friendships.setOnChangedListener {
            if (it == friendship)
                refresh()
        }
    }

    fun show() {
        show(info.player)
    }

    override fun onClick(event: InventoryClickEvent) {
        if (event.rawSlot == inventory.size - 1) {
            back(info.player)
        } else {
            fun add() {
                if (friendship.isParentAdder) {
                    info.player.error(getter["user.friend.sent"])
                } else {
                    friendship.startAt = System.currentTimeMillis()
                    info.player.success(getter["user.friend.added", friendship.name])
                    friendship.friend.messagePool.apply {
                        add(
                            text = "\${user.friend.added,${info.player.name}}",
                            type = MessagePool.Type.System
                        ).let {
                            it.recordTime()
                            val target = friendship.friend
                            if (target.isOnline)
                                it.sendTo(target.onlinePlayerInfo!!)
                        }
                    }
                    back(info.player)
                }
            }

            fun del() {
                val target = friendship.friend
                if (info.friendships.remove(target)) {
                    info.player.success(getter["user.friend.removed", target.name])
                    if (friendship.isFriend) {
                        target.messagePool.add(
                            text = "\$info\${user.friend.wasRemoved,${info.player.name}}",
                            type = MessagePool.Type.System
                        ).also {
                            it.recordTime()
                            if (target.isOnline)
                                it.sendTo(target.onlinePlayerInfo!!)
                        }
                    }
                    back(info.player)
                } else {
                    info.player.error(getter["user.friend.error.notRemoved", target.name])
                }
            }

            if (friendship.isFriend) {
                when (event.rawSlot) {
                    18 -> {
                        PayInputDialog(plugin, info, sellingItem = inventory.getItem(18)!!)
                            .setOnPayListener { success ->
                                if (success) {
                                    val target = friendship.friend

                                    target.currency += amount
                                    friendship.transferred += amount

                                    info.player.success(getter["user.friend.transfer.out", amount, target.name])
                                    target.messagePool.add(
                                        text = "\$success\${user.friend.transfer.in,${info.player.name},$amount}",
                                        type = MessagePool.Type.System
                                    ).let {
                                        it.recordTime()
                                        if (target.isOnline)
                                            it.sendTo(target.onlinePlayerInfo!!)
                                    }
                                } else {
                                    info.player.error(getter["trade.error.poor"])
                                }
                                true
                            }
                            .setOnCancelListener {
                                info.player.info(getter["user.friend.transfer.cancelled"])
                            }
                            .show()
                    }
                    20 -> {
                        info.player.tip(getter["ui.friend.msg.tip", friendship.name])
                        close()
                    }
                    22 -> {
                        if (friendship.sharedInventory == null) {
                            PaymentDialog(
                                player = info.player,
                                sellingItems = SellingItemInfo(
                                    item = ItemStack(Material.CHEST).updateItemMeta<ItemMeta> {
                                        displayName(getter["user.friend.sharingInventory"].toInfoMessage())
                                    },
                                    amount = 1,
                                    unitPrise = 10
                                ),
                                id = TradeManager.getNewID(),
                                plugin = plugin,
                                parentInventory = this
                            )
                                .setOnPayListener { success ->
                                    if (success) {
                                        friendship.createSharedInventory()
                                        info.player.apply {
                                            success(getter["user.friend.inventory.created"])
                                            openInventory(friendship.sharedInventory!!)
                                        }
                                    } else {
                                        info.player.error(getter["trade.error.poor"])
                                    }
                                    true
                                }
                                .setOnCancelListener {
                                    info.player.info(getter["user.friend.inventory.cancelled"])
                                    back(info.player)
                                }
                                .show()
                        } else {
                            info.player.openInventory(friendship.sharedInventory!!)
                        }
                    }
                    24 -> {
                        FriendShareInventory(info, friendship, plugin, this).show(showingTo!!)
                    }
                    26 -> del()
                }
            } else {
                when (event.rawSlot) {
                    18 -> add()
                    20 -> del()
                }
            }
        }
    }
}