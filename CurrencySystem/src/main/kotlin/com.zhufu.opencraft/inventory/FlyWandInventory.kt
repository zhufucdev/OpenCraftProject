package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import com.zhufu.opencraft.special_item.FlyWand
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toTipMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class FlyWandInventory(val player: Player, val wand: FlyWand, plugin: Plugin) : IntractableInventory(plugin) {
    companion object {
        var id: Int = 0
        const val limit = FlyWand.MAX_TIME_REMAINING.toInt() / 60
    }

    override val inventory: Inventory =
        Bukkit.createInventory(null, InventoryType.CHEST, "续费飞行法杖[uuid:${++id}}]".toInfoMessage())

    private fun getLine(i: Int) = 9 + i
    lateinit var subItem: ItemStack
    lateinit var addItem: ItemStack
    lateinit var confirmItem: ItemStack
    var time = 1
    private val price: Int
        get() = time * FlyWand.PRICE_PER_MIN

    private fun preventReachingLimit() {
        if (time + wand.timeRemaining.toInt() / limit > limit) {
            time = 1
        } else if (time <= 0) {
            time = limit - wand.timeRemaining.toInt() / limit
        }
        setShowingItem()
    }

    private fun setShowingItem() {
        inventory.setItem(getLine(4), ItemStack(Material.CLOCK, time)
            .also { confirmItem = it }
            .updateItemMeta<ItemMeta> {
                displayName("续费${time}分钟".toInfoMessage())
                lore(listOf("这将消耗您${price}个货币".toTipMessage(), "点击确认".toTipMessage()))
            }
        )
    }

    init {
        inventory.setItem(getLine(2), ItemStack(Material.NETHER_STAR)
            .also {
                subItem = it
            }
            .updateItemMeta<ItemMeta> {
                displayName("少续费一分钟".toInfoMessage())
            }
        )
        setShowingItem()
        inventory.setItem(getLine(6), ItemStack(Material.NETHER_STAR)
            .also {
                addItem = it
            }
            .updateItemMeta<ItemMeta> {
                displayName("多续费一分钟".toInfoMessage())
            }
        )
        show(player)
    }

    override fun onClick(event: InventoryClickEvent) {
        when (event.currentItem) {
            addItem -> {
                time++
                preventReachingLimit()
            }

            subItem -> {
                time--
                preventReachingLimit()
            }

            confirmItem -> {
                PaymentDialog(
                    player,
                    SellingItemInfo(confirmItem.clone(), 100, time),
                    plugin,
                    this
                )
                    .setOnPayListener { success ->
                        if (success) {
                            wand.updateTime(wand.timeRemaining + time * 60, player.getter())
                            player.success("以为您手中的权杖续费${time}分钟")
                        } else {
                            player.error("交易失败: 您没有足够的货币")
                            cancel()
                        }
                        true
                    }
                    .setOnCancelListener {
                        player.info("交易已取消")
                    }
                    .show()
            }
        }
    }
}