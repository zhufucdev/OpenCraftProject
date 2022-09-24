package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import com.zhufu.opencraft.data.DualInventory
import com.zhufu.opencraft.util.*
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Equipment
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta

class TradeValidateInventory(private val tradeInfo: TradeInfo, face: Location?) : NPCItemInventory(
    tradeInfo.location!!,
    face,
    tradeInfo.item.item.clone().also { it.amount = tradeInfo.item.amount },
    TradeManager.plugin
) {
    override var inventoryName: String = TextUtil.info("确认购买")
    override var inventory: Inventory = Bukkit.createInventory(null, InventoryType.CHEST, inventoryName)
    private var amount = tradeInfo.item.amount
    lateinit var confirmItem: ItemStack
    lateinit var plusItem: ItemStack
    lateinit var subtractItem: ItemStack

    override val clickableNPC: NPC =
        CitizensAPI.getNPCRegistry()
            .createNPC(
                EntityType.ARMOR_STAND,
                TextUtil.info(
                    tradeInfo.seller?.let {
                        PlayerManager.findOfflineInfoByPlayer(it)
                            ?.name
                    }
                        ?: "自定义商人", //TODO Add more social elements
                )
            )

    private var isPaying = false

    init {
        clickableNPC.getOrAddTrait(Equipment::class.java).apply {
            set(
                Equipment.EquipmentSlot.CHESTPLATE,
                ItemStack(Material.LEATHER_CHESTPLATE)
                    .updateItemMeta<LeatherArmorMeta> {
                        setColor(Color.BLUE)
                    })


            set(
                Equipment.EquipmentSlot.LEGGINGS,
                ItemStack(Material.LEATHER_LEGGINGS).updateItemMeta<LeatherArmorMeta> {
                    setColor(Color.BLUE)
                }
            )

            set(
                Equipment.EquipmentSlot.BOOTS,
                ItemStack(Material.LEATHER_BOOTS).updateItemMeta<LeatherArmorMeta> {
                    setColor(Color.BLUE)
                }
            )

            set(
                Equipment.EquipmentSlot.HELMET,
                PlayerManager.findOfflineInfoByPlayer(tradeInfo.seller ?: return@apply)
                    ?.skullItem
                    ?: ItemStack(Material.PLAYER_HEAD)
            )
        }
    }

    private fun setShowingItem() {
        preventReachingLimit()
        inventory.setItem(9 * 1 + 4,
            tradeInfo.item.item.clone().also {
                it.amount = if (amount <= 64) amount else 1
                it.itemMeta = it.itemMeta!!.apply {
                    lore(
                        listOf(
                            "交换${amount}个，共${tradeInfo.item.amount}个".toInfoMessage(),
                            "这将消耗您${tradeInfo.item.unitPrise * amount}个货币".toTipMessage()
                        )
                    )
                }
            }
        )
    }

    private fun preventReachingLimit() {
        if (amount > tradeInfo.item.amount)
            amount = 1
        else if (amount <= 0) {
            amount = tradeInfo.item.amount
        }
    }

    private fun init() {
        inventory.addItem(tradeInfo.item.item.clone().apply { amount = tradeInfo.item.amount })

        setShowingItem()
        inventory.setItem(9 * 1 + 3,
            ItemStack(Material.NETHER_STAR)
                .apply {
                    itemMeta = itemMeta!!.apply {
                        displayName("少交换一个".toInfoMessage())
                    }
                    subtractItem = this
                }
        )
        inventory.setItem(9 * 1 + 5,
            ItemStack(Material.NETHER_STAR)
                .apply {
                    itemMeta = itemMeta!!.apply {
                        displayName("多交换一个".toInfoMessage())
                    }
                    plusItem = this
                }
        )
        inventory.setItem(inventory.size - 1,
            Widgets.confirm.apply {
                itemMeta = itemMeta!!.apply {
                    displayName("确认".toSuccessMessage())
                }
                confirmItem = this
            }
        )
    }

    override fun onItemClick(event: InventoryClickEvent): Boolean {
        if (inventoryName.startsWith("修改")) {
            if (event.currentItem!!.type != Material.AIR && event.currentItem!!.type != tradeInfo.item!!.item.type) {
                event.whoClicked.error("抱歉，但您不能同时出售两种不同的物品")
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

    override fun preInventoryOpen(player: HumanEntity) {
        val info = PlayerManager.findInfoByPlayer(player.uniqueId)
        if (info == null) {
            player.error(Language.getDefault("player.error.unknown"))
            return
        }

        if (player.world == Base.tradeWorld)
            info.inventory.getOrCreate("survivor").load(inventoryOnly = true)

        if (player.uniqueId == (tradeInfo.seller ?: return)) {
            inventoryName = "修改物品销售"
            inventory = Bukkit.createInventory(null, InventoryType.CHEST, inventoryName)
            inventory.addItem(tradeInfo.item.item.clone().also { it.amount = tradeInfo.item.amount })
        } else {
            inventoryName = "确认购买"
            init()
        }
    }

    override fun onInventoryClose(player: HumanEntity) {
        val info = PlayerManager.findInfoByPlayer(player.uniqueId)
        if (info == null) {
            player.error(Language.getDefault("player.error.unknown"))
            return
        }
        if (inventoryName.startsWith("修改")) {
            val type = tradeInfo.item.item.type
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
                player.error("抱歉，但您不能同时出售两种不同的物品")
            }
            if (amount != 0) {
                if (TradeManager.checkLimit(tradeInfo.item.unitPrise, amount)) {
                    player.inventory.addItem(tradeInfo.item.item.also {
                        it.amount = amount - tradeInfo.item.amount
                    })
                    player.error("总价超过上限，尝试减少销售数量")
                    player.info("修改未保存")
                } else {
                    tradeInfo.item.amount = amount
                    player.success("修改已保存")
                }
            } else {
                tradeInfo.cancel()
                player.info("已取消物品销售")
            }
            inventory.clear()
        } else if (!isPaying) {
            TradeManager.loadTradeCompass(info)
        }
        if (!TradeTerritoryInfo(info).contains(player.location) && player.world == Base.tradeWorld)
            info.inventory.getOrCreate(DualInventory.RESET).load(inventoryOnly = true)
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
            tradeInfo.item.clone().also { it.amount = amount },
            TradeManager.plugin
        )
            .setOnPayListener { success ->
                if (success) {
                    val info = player.info()!!
                    when (TradeManager.buy(player, tradeInfo, amount)) {
                        TradeManager.TradeResult.FAILED -> {
                            return@setOnPayListener false
                        }

                        TradeManager.TradeResult.SUCCESSFUL -> {
                            tradeInfo.destroy()
                        }

                        TradeManager.TradeResult.UPDATE -> {
                            init()
                        }
                    }
                    TradeManager.loadTradeCompass(info)
                } else {
                    player.error(player.getter()["trade.error.poor"])
                }
                isPaying = false
                true
            }
            .setOnCancelListener {
                player.info("交易已取消")
                isPaying = false
            }
            .show()
    }
}