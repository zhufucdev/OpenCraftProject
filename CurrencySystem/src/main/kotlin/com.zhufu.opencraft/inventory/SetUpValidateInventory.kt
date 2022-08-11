package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toSuccessMessage
import com.zhufu.opencraft.util.toTipMessage
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class SetUpValidateInventory(baseLocation: Location, itemSell: ItemStack, private val player: Player) :
    NPCItemInventory(baseLocation, null, itemSell, CurrencySystem.instance) {
    companion object {
        val inventories = ArrayList<SetUpValidateInventory>()
    }

    private var confirmItem: ItemStack
    private var cancelItem: ItemStack
    private var giveSignItem: ItemStack
    override var inventoryName: String = TextUtil.tip("确认销售[uuid:$id]")
    override var inventory: Inventory = Bukkit.createInventory(null, InventoryType.CHEST, inventoryName)
    var items = SellingItemInfo(itemSell, -1, itemSell.amount)

    init {
        inventory.addItem(itemSell)
        inventory.setItem(inventory.size - 1,
            Widgets.confirm
                .also { itemStack ->
                    itemStack.itemMeta = itemStack.itemMeta!!.also {
                        it.displayName(
                            "确认".toSuccessMessage()
                        )
                    }
                    confirmItem = itemStack
                }
        )
        inventory.setItem(inventory.size - 2,
            ItemStack(Material.BLUE_DYE)
                .also { itemStack ->
                    giveSignItem = itemStack
                }
                .updateItemMeta<ItemMeta> {
                    displayName("获取告示牌".toInfoMessage())
                    lore(listOf("请在白色玻璃周围放置告示牌并在首行写入单价".toTipMessage()))
                }
        )
        inventory.setItem(inventory.size - 3,
            Widgets.cancel
                .also { itemStack ->
                    cancelItem = itemStack
                }
                .updateItemMeta<ItemMeta> {
                    displayName("取消".toInfoMessage())
                }
        )
        baseLocation.block.type = Material.BLACK_STAINED_GLASS
        inventories.add(this)
    }

    override fun onItemClick(event: InventoryClickEvent): Boolean {
        if (event.whoClicked == player) {
            when (event.currentItem) {
                confirmItem -> confirm()
                cancelItem -> cancel(event.whoClicked)
                giveSignItem -> giveSign()
            }
        }
        return true
    }

    private fun giveSign() {
        player.closeInventory()
        inventory.clear(inventory.size - 2)
        player.inventory.addItem(ItemStack(Material.ACACIA_SIGN))
    }

    override fun cancel(player: HumanEntity?) {
        player?.inventory?.addItem(items.item.clone().also { it.amount = items.amount })
        signPossible.forEach {
            if (it.block.type.name.contains("wall_sign", true)) {
                it.block.type = Material.AIR
            }
        }
        baseLocation.block.type = Material.AIR

        inventories.removeIf { it.validateInventory(inventory) }
        super.cancel(player)
    }

    fun destroy(player: HumanEntity?) {
        super.cancel(player)
        baseLocation.block.type = Material.AIR
    }

    private val signPossible
        get() = arrayOf(
            baseLocation.clone().add(1.0, 0.0, 0.0), baseLocation.clone().add(-1.0, 0.0, 0.0),
            baseLocation.clone().add(0.0, 0.0, 1.0), baseLocation.clone().add(0.0, 0.0, -1.0)
        )

    private fun confirm(): Boolean {
        val block = signPossible.firstOrNull { it.block.type.name.contains("wall_sign", true) }?.block
        if (block == null) {
            player.closeInventory()
            player.error("未在白色玻璃周围发现告示牌，请放置告示牌并在首行写入单价(整数)")
            return false
        }
        val sign = block.state as Sign

        val line = (sign.line(0) as TextComponent).content()
        val unitPrise = line.toLongOrNull()
        if (unitPrise == null) {
            player.error("无效数据: $line")
            return false
        }
        items.unitPrise = unitPrise
        if (!TradeManager.checkLimit(items)) {
            TradeManager.sell(
                seller = player.uniqueId.toString(),
                what = items.item,
                amount = items.amount,
                unitPrise = line.toLong(),
                ignoreInventoryItem = true,
                location = baseLocation
            )

            //Clean
            sign.block.type = Material.AIR
            destroy(player)
            return true
        } else {
            player.error("总价超过上限，尝试减少销售数量或是降低单价")
            return false
        }
    }
}