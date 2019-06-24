package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.ClickableInventory
import com.zhufu.opencraft.SellingItemInfo
import com.zhufu.opencraft.TextUtil
import com.zhufu.opencraft.Widgets
import org.bukkit.Bukkit
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

class PaymentDialog(val player: HumanEntity, private val sellingItems: SellingItemInfo, id: Int,plugin: Plugin) : ClickableInventory(plugin) {
    override val inventory: Inventory = Bukkit.createInventory(null, InventoryType.CHEST,TextUtil.info("确认支付[uuid:$id]"))
    lateinit var confirmItem: ItemStack
    lateinit var cancelItem: ItemStack

    fun show(){
        inventory.addItem(sellingItems.item.clone()
                .also { itemStack ->
                    val targetAmount = sellingItems.amount
                    if (targetAmount <= 3*64)
                        itemStack.amount = sellingItems.amount
                    else {
                        itemStack.amount = 3 * 64
                        itemStack.itemMeta = itemStack.itemMeta.also { it!!.lore = listOf(TextUtil.tip("数量为${targetAmount}个")) }
                    }
                })
        inventory.setItem(
                inventory.size - 1,
                Widgets.confirm.also { itemStack ->
                            itemStack.itemMeta = itemStack.itemMeta!!
                                    .also {
                                        it.setDisplayName(TextUtil.getColoredText("确认", TextUtil.TextColor.GREEN, true, false))
                                        it.lore = listOf(TextUtil.tip("这将消耗您${sellingItems.prise}个货币"))
                                    }
                            confirmItem = itemStack
                        }
        )
        inventory.setItem(
                inventory.size - 2,
                Widgets.cancel.also { itemStack ->
                            itemStack.itemMeta = itemStack.itemMeta!!
                                    .also { it.setDisplayName(TextUtil.getColoredText("取消", TextUtil.TextColor.RED, true, false)) }
                            cancelItem = itemStack
                        }
        )

        player.closeInventory()
        show(player)
    }

    fun hide(){
        if (!isShowing)
            return
        isShowing = false
        player.closeInventory()
    }

    fun cancel(){
        hide()
        onCancelListener?.invoke(this)
    }

    fun confirm(){
        hide()
        onConfirmListener?.invoke(this)
    }

    private var onConfirmListener: ((PaymentDialog) -> Unit)? = null
    private var onCancelListener: ((PaymentDialog) -> Unit)? = null
    fun setOnConfirmListener(l: (PaymentDialog) -> Unit): PaymentDialog {
        onConfirmListener = l
        return this
    }
    fun setOnCancelListener(l: (PaymentDialog) -> Unit): PaymentDialog {
        onCancelListener = l
        return this
    }

    override fun onClick(event: InventoryClickEvent){
        when (event.currentItem) {
            cancelItem -> {
                cancel()
            }
            confirmItem -> {
                confirm()
            }
        }
    }

    override fun onClose(player: HumanEntity) {
        if (!isShowing)
            return
        cancel()
    }
}

