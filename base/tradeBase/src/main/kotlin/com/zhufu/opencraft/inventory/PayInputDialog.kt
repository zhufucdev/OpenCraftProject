package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.util.toInfoMessage
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

class PayInputDialog(plugin: Plugin, private val info: Info, private val sellingItem: ItemStack) :
    KeypadInventory(plugin, info.getter(), info.getter()["trade.input.title"].toInfoMessage()) {
    override fun onConfirm() {
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

    fun setOnPayListener(l: PayInputDialog.(Boolean) -> Boolean): PayInputDialog {
        onPay = l
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