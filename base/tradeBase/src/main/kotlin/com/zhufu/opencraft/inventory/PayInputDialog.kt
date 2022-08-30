package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.util.toInfoMessage
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

class PayInputDialog(
    plugin: Plugin,
    private val info: Info,
    private val sellingItem: ItemStack,
    override val parentInventory: IntractableInventory? = null
) :
    KeypadInventory(plugin, info.getter(), info.getter()["trade.input.title"].toInfoMessage()), Backable {
    override fun onConfirm() {
        if (onConfirm?.invoke(this) == false) {
            if (parentInventory == null)
                close()
            else
                back(showingTo!!)
            return
        }
        PaymentDialog(
            sellingItems = SellingItemInfo(
                item = sellingItem,
                unitPrise = amount.toLong(),
                amount = 1
            ),
            player = info.player,
            id = TradeManager.getNewID(),
            plugin = plugin
        )
            .setOnPayListener { s ->
                onPay?.invoke(this@PayInputDialog, s) ?: true
            }
            .setOnCancelListener {
                onCancel?.invoke()
            }
            .show()
    }

    private var onPay: (PayInputDialog.(Boolean) -> Boolean)? = null
    private var onCancel: (() -> Unit)? = null
    private var onConfirm: (PayInputDialog.() -> Boolean)? = null

    /**
     * Listen for player pay
     * @param l Parameters are __(Success)__ -> __CarryOn__
     */
    fun setOnPayListener(l: PayInputDialog.(Boolean) -> Boolean): PayInputDialog {
        onPay = l
        return this
    }

    /**
     * Listen for player confirm pay, which happens
     * before payment actually happens.
     * @param l Return `true` if actual payment should be carried,
     * `false` otherwise.
     */
    fun setOnConfirmListener(l: PayInputDialog.() -> Boolean): PayInputDialog {
        onConfirm = l
        return this
    }

    fun setOnCancelListener(l: () -> Unit): PayInputDialog {
        onCancel = l
        return this
    }

    fun show() = show(info.player)

    override fun onKeyDown(index: Int) {
        if (amount > Int.MAX_VALUE / 10 || amount < 0) {
            close()
            info.player.error(getter["trade.input.error.outOfBound", Int.MAX_VALUE / 10])
        }
    }
}