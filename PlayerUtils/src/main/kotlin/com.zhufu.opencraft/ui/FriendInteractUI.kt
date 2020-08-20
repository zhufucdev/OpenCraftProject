package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.inventory.PayInputDialog
import com.zhufu.opencraft.inventory.PaymentDialog
import com.zhufu.opencraft.player_community.FriendWrap
import com.zhufu.opencraft.player_community.MessagePool
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class FriendInteractUI(
    plugin: Plugin,
    private val friend: FriendWrap,
    private val info: Info,
    override val parentInventory: ClickableInventory
) : ClickableInventory(plugin), Backable {
    override val inventory: Inventory = Bukkit.createInventory(null, 36)
    val getter by lazy { Language.LangGetter(friend.parent) }

    private fun refresh() {
        if (friend.exits)
            inventory.apply {
                setItem(13, friend.friend.skullItem.updateItemMeta<ItemMeta> {
                    setDisplayName(friend.name)
                    if (friend.isFriend)
                        lore = arrayListOf<String>().apply {
                            friend.statics(getter).forEach {
                                add(it.toInfoMessage())
                            }
                        }
                })
                val deleteItem = Widgets.cancel.updateItemMeta<ItemMeta> {
                    setDisplayName(getter["ui.friend.del"].toErrorMessage())
                }
                if (friend.isFriend) {
                    setItem(18, ItemStack(Material.GOLD_INGOT).updateItemMeta<ItemMeta> {
                        setDisplayName(getter["ui.friend.transfer.title"].toInfoMessage())
                        lore = listOf(getter["ui.friend.transfer.click"].toTipMessage())
                    })
                    setItem(20, ItemStack(Material.OAK_SIGN).updateItemMeta<ItemMeta> {
                        setDisplayName(getter["ui.friend.msg.title"].toInfoMessage())
                        lore = listOf(getter["ui.friend.msg.tip", friend.name].toTipMessage())
                    })
                    setItem(22, ItemStack(Material.CHEST).updateItemMeta<ItemMeta> {
                        setDisplayName(getter["ui.friend.inventory.title"].toInfoMessage())
                        lore = listOf(
                            getter["ui.friend.inventory.${if (friend.sharedInventory == null) "buy" else "check"}"].toTipMessage()
                        )
                    })
                    setItem(24, ItemStack(Material.CAKE).updateItemMeta<ItemMeta> {
                        setDisplayName(getter["ui.friend.share.title"].toInfoMessage())
                        lore = listOf(getter["ui.friend.share.click"].toTipMessage())
                    })
                    setItem(26, deleteItem)
                } else {
                    setItem(18, Widgets.confirm.updateItemMeta<ItemMeta> {
                        setDisplayName(getter["ui.friend.add"].toSuccessMessage())
                    })
                    setItem(20, deleteItem)
                }
                setItem(inventory.size - 1, Widgets.back.updateItemMeta<ItemMeta> {
                    setDisplayName(getter["ui.back"].toInfoMessage())
                })
            }
        else {
            (parentInventory as FriendListUI).refresh()
            back(info.player)
        }
    }

    init {
        refresh()
        info.friendship.setOnChangedListener {
            if (it == friend)
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
                if (friend.isParentAdder) {
                    info.player.error(getter["user.friend.sent"])
                } else {
                    friend.startAt = System.currentTimeMillis()
                    info.player.success(getter["user.friend.added", friend.name])
                    friend.friend.messagePool.apply {
                        add(
                            text = "\${user.friend.added,${info.player.name}}",
                            type = MessagePool.Type.System
                        ).let {
                            it.recordTime()
                            val target = friend.friend
                            if (target.isOnline)
                                it.sendTo(target.onlinePlayerInfo!!)
                        }
                    }
                    back(info.player)
                }
            }

            fun del() {
                val target = friend.friend
                if (info.friendship.remove(target)) {
                    info.player.success(getter["user.friend.removed", target.name])
                    if (friend.isFriend) {
                        target.messagePool.add(
                            text = "\$info\${user.friend.wasRemoved,${info.player.name}}",
                            type = MessagePool.Type.System
                        ).let {
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

            if (friend.isFriend) {
                when (event.rawSlot) {
                    18 -> {
                        PayInputDialog(plugin, info, sellingItem = inventory.getItem(18)!!)
                            .setOnPayListener { success ->
                                if (success) {
                                    val target = friend.friend

                                    target.currency += amount
                                    friend.transferred += amount

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
                        info.player.tip(getter["ui.friend.msg.tip", friend.name])
                        close()
                    }
                    22 -> {
                        if (friend.sharedInventory == null) {
                            PaymentDialog(
                                player = info.player,
                                sellingItems = SellingItemInfo(
                                    item = ItemStack(Material.CHEST).updateItemMeta<ItemMeta> {
                                        setDisplayName(getter["user.friend.sharingInventory"].toInfoMessage())
                                    },
                                    amount = 1,
                                    unitPrise = GeneralPrise(10)
                                ),
                                plugin = plugin
                            )
                                .setOnPayListener { success ->
                                    if (success) {
                                        friend.createSharedInventory()
                                        info.player.apply {
                                            success(getter["user.friend.inventory.created"])
                                            openInventory(friend.sharedInventory!!)
                                        }
                                    } else {
                                        info.player.error(getter["trade.error.poor"])
                                    }
                                    true
                                }
                                .setOnCancelListener {
                                    info.player.info(getter["user.friend.inventory.cancelled"])
                                }
                                .show()
                        } else {
                            info.player.openInventory(friend.sharedInventory!!)
                        }
                    }
                    24 -> {
                        FriendShareInventory(info, friend, plugin, this).show(showingTo!!)
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