package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import com.zhufu.opencraft.util.*
import org.bukkit.Bukkit
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class PaymentDialog(val player: HumanEntity, private val sellingItems: SellingItemInfo, id: Int, plugin: Plugin) :
    ClickableInventory(plugin) {
    override val inventory: Inventory =
        Bukkit.createInventory(null, InventoryType.CHEST, "确认支付[uuid:$id]".toInfoMessage())
    lateinit var confirmItem: ItemStack
    lateinit var cancelItem: ItemStack
    private val getter = player.getter()

    fun show() {
        inventory.addItem(sellingItems.item.clone()
            .also { itemStack ->
                val targetAmount = sellingItems.amount
                if (targetAmount <= 3 * 64)
                    itemStack.amount = sellingItems.amount
                else {
                    itemStack.amount = 3 * 64
                    itemStack.updateItemMeta<ItemMeta> {
                        lore(listOf("数量为${targetAmount}个".toInfoMessage()))
                    }
                }
            })
        inventory.setItem(
            inventory.size - 1,
            Widgets.confirm.also { itemStack ->
                itemStack.itemMeta = itemStack.itemMeta!!
                    .also {
                        it.displayName(getter["trade.pay.saving"].toSuccessMessage())
                        it.lore(listOf(getter["trade.pay.currencyConsume", sellingItems.prise].toTipMessage()))
                    }
                confirmItem = itemStack
            }
        )
        inventory.setItem(
            inventory.size - 2,
            Widgets.confirm.updateItemMeta<ItemMeta> {
                displayName(getter["trade.pay.cash"].toSuccessMessage())
                lore(listOf(getter["trade.pay.currencyConsume", sellingItems.prise].toInfoMessage()))
            }
        )
        inventory.setItem(
            inventory.size - 3,
            Widgets.cancel.also { itemStack ->
                itemStack.itemMeta = itemStack.itemMeta!!
                    .also { it.displayName("取消".toErrorMessage()) }
                cancelItem = itemStack
            }
        )

        player.closeInventory()
        show(player)
    }

    fun hide() {
        if (!isShowing)
            return
        isShowing = false
        player.closeInventory()
    }

    fun cancel() {
        hide()
        onCancelListener?.invoke(this)
    }

    fun confirm(isCash: Boolean = false) {
        hide()
        onConfirmListener?.invoke(this, isCash)

        if (isCash) {
            if (onPayListener?.invoke(this, player.addCash(-sellingItems.prise.toInt())) == false)
                player.addCash(sellingItems.prise.toInt())
        } else {
            val info = player.info()
            if (info == null) {
                player.error(Language.getDefault("player.error.unknown"))
            } else {
                if (info.currency >= sellingItems.prise) {
                    if (onPayListener?.invoke(this, true) != false)
                        info.currency -= sellingItems.prise
                } else {
                    onPayListener?.invoke(this, false)
                }
            }
        }
    }

    private var onConfirmListener: (PaymentDialog.(isCash: Boolean) -> Unit)? = null
    private var onCancelListener: (PaymentDialog.() -> Unit)? = null
    private var onPayListener: (PaymentDialog.(Boolean) -> Boolean)? = null
    fun setOnConfirmListener(l: PaymentDialog.(Boolean) -> Unit): PaymentDialog {
        onConfirmListener = l
        return this
    }

    fun setOnCancelListener(l: PaymentDialog.() -> Unit): PaymentDialog {
        onCancelListener = l
        return this
    }

    fun setOnPayListener(l: PaymentDialog.(Boolean) -> Boolean): PaymentDialog {
        onPayListener = l
        return this
    }

    override fun onClick(event: InventoryClickEvent) {
        when (event.rawSlot) {
            inventory.size - 3 -> {
                cancel()
            }
            inventory.size - 2 -> {
                confirm(true)
            }
            inventory.size - 1 -> {
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

