package com.zhufu.opencraft.headers.player_wrap

import com.zhufu.opencraft.DualInventory
import com.zhufu.opencraft.Info
import com.zhufu.opencraft.ServerPlayer
import com.zhufu.opencraft.getLang
import java.util.function.Function

class PlayerInventory private constructor(private val inventory: DualInventory.InventoryInfo) {
    private val online get() = inventory.player != null
    val name get() = inventory.name
    val items: List<SimpleItemStack?> get() {
        sync(fromMode = true)
        return inventory.items().toSimpleItemStackList()
    }
    fun sort(compare: Function<SimpleItemStack?, Long>) {
        val sorted = items.sortedBy { compare.apply(it) }
        sorted.forEachIndexed { index, item ->
            inventory.setItem(
                index,
                SimpleItemStack.recover(item)
            )
        }

        sync()
    }

    fun sort() {
        sort(Function { return@Function it?.getType()?.ordinal?.toLong() ?: Long.MAX_VALUE })
    }

    private fun sync(fromMode: Boolean = false) {
        if (!online)
            return
        if (!fromMode) {
            items.forEachIndexed { index, item ->
                inventory.player!!.inventory.setItem(index, SimpleItemStack.recover(item))
            }
        } else {
            inventory.player!!.inventory.forEachIndexed { index, itemStack ->
                inventory.setItem(index, itemStack)
            }
        }
    }

    companion object {
        fun of(playerInfo: ServerPlayer, name: String? = null): PlayerInventory {
            val dual = if (playerInfo is Info) playerInfo.inventory else DualInventory(null, playerInfo)
            val inventory =
                if (name != null) dual.create(name)
                else {
                    if (playerInfo is Info)
                        dual.present
                    else
                        throw IllegalArgumentException(getLang(playerInfo, "scripting.error.parWhenOffline", "name"))
                }
            inventory.save()
            return PlayerInventory(inventory)
        }
    }
}