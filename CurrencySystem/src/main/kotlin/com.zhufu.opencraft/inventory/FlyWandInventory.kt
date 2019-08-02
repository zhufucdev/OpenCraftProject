package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import com.zhufu.opencraft.special_item.FlyWand
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

class FlyWandInventory(val player: Player, plugin: Plugin) : ClickableInventory(plugin) {
    companion object {
        var id: Int = 0
        const val limit = FlyWand.MAX_TIME_REMAINING.toInt() / 60
    }

    override val inventory: Inventory =
        Bukkit.createInventory(null, InventoryType.CHEST, TextUtil.info("续费飞行法杖[uuid:${++id}}]"))

    private fun getLine(i: Int) = 9 + i
    lateinit var subItem: ItemStack
    lateinit var addItem: ItemStack
    lateinit var confirmItem: ItemStack
    val wand = if (FlyWand.isThis(player.inventory.itemInMainHand)) FlyWand(
        player.inventory.itemInMainHand, player.getter()
    ) else null
    var time = 1
    private val price: Int
        get() = time * FlyWand.PRICE_PER_MIN

    private fun preventReachingLimit() {
        if (time + wand!!.timeRemaining.toInt() / limit > limit) {
            time = 1
        } else if (time <= 0) {
            time = limit - wand.timeRemaining.toInt() / limit
        }
        setShowingItem()
    }

    private fun setShowingItem() {
        inventory.setItem(getLine(4), ItemStack(Material.CLOCK, time)
            .also {
                it.itemMeta = it.itemMeta!!.apply {
                    setDisplayName(TextUtil.info("续费${time}分钟"))
                    lore = listOf(TextUtil.tip("这将消耗您${price}个货币"), TextUtil.tip("点击确认"))
                }
                confirmItem = it
            }
        )
    }

    init {
        if (wand != null) {
            inventory.setItem(getLine(2), ItemStack(Material.NETHER_STAR)
                .also {
                    it.itemMeta = it.itemMeta!!.apply {
                        setDisplayName(TextUtil.tip("少续费一分钟"))
                    }
                    subItem = it
                }
            )
            setShowingItem()
            inventory.setItem(getLine(6), ItemStack(Material.NETHER_STAR)
                .also {
                    it.itemMeta = it.itemMeta!!.apply {
                        setDisplayName(TextUtil.tip("多续费一分钟"))
                    }
                    addItem = it
                }
            )
            show(player)
        } else {
            player.sendMessage(TextUtil.error("您必须将权杖拿在主手"))
        }
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
                PaymentDialog(player, SellingItemInfo(confirmItem.clone(), 100, time), TradeManager.getNewID(), plugin)
                    .setOnPayListener { success ->
                        if (success) {
                            player.inventory.setItem(
                                player.inventory.heldItemSlot,
                                wand!!.also { meta -> meta.updateTime(wand.timeRemaining + time * 60) })
                            player.sendMessage(TextUtil.success("以为您手中的权杖续费${time}分钟"))
                        } else {
                            player.sendMessage(TextUtil.error("交易失败: 您没有足够的货币"))
                            cancel()
                        }
                        true
                    }
                    .setOnCancelListener {
                        player.sendMessage(TextUtil.info("交易已取消"))
                    }
                    .show()
            }
        }
    }
}