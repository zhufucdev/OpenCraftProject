package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import org.bukkit.Bukkit
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin
import kotlin.math.cos

class PaymentDialog(val player: Player, private val sellingItems: SellingItemInfo, plugin: Plugin) :
    ClickableInventory(plugin) {
    private val getter = player.getter()
    override val inventory: Inventory =
        Bukkit.createInventory(null, InventoryType.CHEST, TextUtil.info(getter["trade.pay.title"]))
    lateinit var confirmItem: ItemStack
    lateinit var cancelItem: ItemStack

    fun show() {
        inventory.addItem(sellingItems.item.clone()
            .also { itemStack ->
                val targetAmount = sellingItems.amount
                if (targetAmount <= 3 * 64)
                    itemStack.amount = sellingItems.amount
                else {
                    itemStack.amount = 3 * 64
                    itemStack.itemMeta =
                        itemStack.itemMeta.also {
                            it!!.lore = listOf(TextUtil.tip(getter["trade.pay.amount", targetAmount]))
                        }
                }
            })
        inventory.setItem(
            inventory.size - 1,
            Widgets.confirm.also { itemStack ->
                itemStack.itemMeta = itemStack.itemMeta!!
                    .also {
                        it.setDisplayName(
                            TextUtil.getColoredText(
                                getter["trade.pay.saving"],
                                TextUtil.TextColor.GREEN,
                                true,
                                false
                            )
                        )
                        it.lore = listOf(TextUtil.tip(getter["trade.pay.currencyConsume", sellingItems.prise.toString(getter)]))
                    }
                confirmItem = itemStack
            }
        )
        inventory.setItem(
            inventory.size - 2,
            Widgets.confirm.updateItemMeta<ItemMeta> {
                setDisplayName(TextUtil.getColoredText(getter["trade.pay.cash"], TextUtil.TextColor.GREEN, true, false))
                lore = listOf(TextUtil.tip(getter["trade.pay.currencyConsume", sellingItems.prise.toString(getter)]))
            }
        )
        inventory.setItem(
            inventory.size - 3,
            Widgets.cancel.also { itemStack ->
                itemStack.itemMeta = itemStack.itemMeta!!
                    .also {
                        it.setDisplayName(
                            TextUtil.getColoredText(
                                getter["ui.cancel"],
                                TextUtil.TextColor.RED,
                                true,
                                false
                            )
                        )
                    }
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

    private fun confirm(isCash: Boolean = false) {
        hide()
        onConfirmListener?.invoke(this, isCash)

        if (isCash) {
            val cost = sellingItems.prise.cost(player)
            if (onPayListener?.invoke(this, cost.successful) == false && cost.successful) {
                cost.undo()
            }
        } else {
            val info = player.info()
            if (info == null) {
                player.error(Language.getDefault("player.error.unknown"))
            } else {
                val cost = sellingItems.prise.cost(info)
                if (onPayListener?.invoke(this, cost.successful) == false && cost.successful) {
                    cost.undo()
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

    /**
     * Set the listener triggered when the payment is about to be done.
     * @param l return true if the payment should be done, false otherwise.
     */
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

