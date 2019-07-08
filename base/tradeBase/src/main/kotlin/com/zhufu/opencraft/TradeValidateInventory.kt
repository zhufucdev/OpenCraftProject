package com.zhufu.opencraft

import com.zhufu.opencraft.inventory.PaymentDialog
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

class TradeValidateInventory(val tradeInfo: TradeInfo, face: Location?) : NPCItemInventory(
    tradeInfo.location!!, face, tradeInfo.items!!.item.clone().also { it.amount = tradeInfo.items!!.amount },
    TradeManager.plugin!!
) {
    override var inventoryName: String = "确认购买$id"
    override var inventory: Inventory = Bukkit.createInventory(null, InventoryType.CHEST, inventoryName)
    private var amount = tradeInfo.items!!.amount
    lateinit var confirmItem: ItemStack
    lateinit var plusItem: ItemStack
    lateinit var subtractItem: ItemStack

    private var isPaying = false

    init {
        (clickableNPC.entity as ArmorStand)
            .apply {
                setHelmet(ItemStack(Material.PLAYER_HEAD).apply {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
                        itemMeta = (itemMeta as SkullMeta).also { meta ->
                            meta.owningPlayer = Bukkit.getOfflinePlayer(UUID.fromString(tradeInfo.getSeller()))
                        }
                    }
                })
                setChestplate(ItemStack(Material.LEATHER_CHESTPLATE).apply {
                    itemMeta = (itemMeta as LeatherArmorMeta).also { meta -> meta.setColor(Color.BLUE) }
                })
                setLeggings(ItemStack(Material.LEATHER_LEGGINGS).apply {
                    itemMeta = (itemMeta as LeatherArmorMeta).also { meta -> meta.setColor(Color.BLUE) }
                })
                setBoots(ItemStack(Material.LEATHER_BOOTS).apply {
                    itemMeta = (itemMeta as LeatherArmorMeta).also { meta -> meta.setColor(Color.BLUE) }
                })
            }
    }

    private fun setShowingItem() {
        preventReachingLimit()
        inventory.setItem(9 * 1 + 4,
            tradeInfo.items!!.item.clone().also {
                it.amount = if (amount <= 64) amount else 1
                it.itemMeta = it.itemMeta!!.apply {
                    lore = listOf(
                        TextUtil.info("交换${amount}个，共${tradeInfo.items!!.amount}个"),
                        TextUtil.tip("这将消耗您${tradeInfo.items!!.unitPrise * amount}个货币")
                    )
                }
            }
        )
    }

    private fun preventReachingLimit() {
        if (amount > tradeInfo.items!!.amount)
            amount = 1
        else if (amount <= 0) {
            amount = tradeInfo.items!!.amount
        }
    }

    private fun init() {
        inventory.addItem(tradeInfo.items!!.item.clone().also { it.amount = tradeInfo.items!!.amount })

        setShowingItem()
        inventory.setItem(9 * 1 + 3,
            ItemStack(Material.NETHER_STAR)
                .apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(
                            TextUtil.getColoredText(
                                "少交换一个", TextUtil.TextColor.GOLD, false,
                                underlined = false
                            )
                        )
                    }
                    subtractItem = this
                }
        )
        inventory.setItem(9 * 1 + 5,
            ItemStack(Material.NETHER_STAR)
                .apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(
                            TextUtil.getColoredText(
                                "多交换一个", TextUtil.TextColor.GOLD, false,
                                underlined = false
                            )
                        )
                    }
                    plusItem = this
                }
        )
        inventory.setItem(inventory.size - 1,
            Widgets.confirm.apply {
                itemMeta = itemMeta!!.apply {
                    setDisplayName(
                        TextUtil.getColoredText(
                            "确认", TextUtil.TextColor.GREEN, true,
                            underlined = false
                        )
                    )
                }
                confirmItem = this
            }
        )
    }

    override fun onItemClick(event: InventoryClickEvent): Boolean {
        if (inventoryName.startsWith("修改")) {
            if (event.currentItem!!.type != Material.AIR && event.currentItem!!.type != tradeInfo.items!!.item.type) {
                event.whoClicked.sendMessage(TextUtil.error("抱歉，但您不能同时出售两种不同的物品"))
                return true
            }
            return false
        } else {
            when (event.currentItem) {
                confirmItem -> confirm(event.whoClicked)
                plusItem -> plusOne()
                subtractItem -> subtractOne()
            }
        }
        return true
    }

    override fun onInventoryOpen(player: HumanEntity) {
        val info = PlayerManager.findInfoByPlayer(player.uniqueId)
        if (info == null) {
            player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
            return
        }

        if (player.uniqueId.toString() == tradeInfo.getSeller() ?: return) {
            inventoryName = "修改$id"
            inventory = Bukkit.createInventory(null, InventoryType.CHEST, inventoryName)
            inventory.addItem(tradeInfo.items!!.item.clone().also { it.amount = tradeInfo.items!!.amount })
        } else {
            init()
        }
    }

    override fun onInventoryClose(player: HumanEntity) {
        val info = PlayerManager.findInfoByPlayer(player.uniqueId)
        if (info == null) {
            player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
            return
        }
        if (inventoryName.startsWith("修改")) {
            val type = tradeInfo.items!!.item.type
            var diff = false
            var amount = 0
            inventory.forEach {
                if (it != null) {
                    if (it.type != type) {
                        player.inventory.addItem(it)
                        diff = true
                    } else amount += it.amount
                }
            }
            if (diff) {
                player.sendMessage(TextUtil.error("抱歉，但您不能同时出售两种不同的物品"))
            }
            if (amount != 0) {
                if (TradeManager.checkLimit(tradeInfo.items!!.unitPrise, amount)) {
                    player.inventory.addItem(tradeInfo.items!!.item.also {
                        it.amount = amount - tradeInfo.items!!.amount
                    })
                    player.sendMessage(TextUtil.error("总价超过上限，尝试减少销售数量"))
                    player.sendMessage(TextUtil.info("您的修改未保存"))
                } else {
                    tradeInfo.items!!.amount = amount
                    player.sendMessage(TextUtil.success("您的修改已保存"))
                }
            } else {
                TradeManager.destroy(tradeInfo)
                player.sendMessage(TextUtil.info("已取消物品销售"))
            }
            inventory.clear()
        } else if (!isPaying) {
            TradeManager.loadTradeCompass(info)
        }
    }

    private fun plusOne() {
        amount++
        setShowingItem()
    }

    private fun subtractOne() {
        amount--
        setShowingItem()
    }

    private fun confirm(player: HumanEntity) {
        isPaying = true
        PaymentDialog(
            player,
            tradeInfo.items!!.clone().also { it.amount = amount },
            tradeInfo.id,
            TradeManager.plugin!!
        )
            .setOnConfirmListener {
                val info = PlayerManager.findInfoByPlayer(player.uniqueId)
                if (info == null) {
                    player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
                    return@setOnConfirmListener
                }
                when (TradeManager.buy(player, tradeInfo.id, amount)) {
                    TradeManager.TradeResult.FAILED -> {
                        return@setOnConfirmListener
                    }
                    TradeManager.TradeResult.SUCCESSFUL -> {
                        TradeManager.destroy(tradeInfo)
                    }
                    TradeManager.TradeResult.UPDATE -> {
                        init()
                    }
                }
                TradeManager.loadTradeCompass(info)
                isPaying = false
            }
            .setOnCancelListener {
                player.sendMessage(TextUtil.info("交易已取消"))
                isPaying = false
            }
            .show()
    }
}