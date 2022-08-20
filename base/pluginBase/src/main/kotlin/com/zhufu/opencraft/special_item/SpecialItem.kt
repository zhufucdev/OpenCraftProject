package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.util.Language
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * A self-programmed item.
 */
abstract class SpecialItem(m: Material, typeID: UUID): ItemStack(m) {
    init {
        val nbt = NBTItem(this, true)
        nbt.setUUID(KEY_SIID, typeID)
    }

    var inventoryPosition: Int = -1
    var holder: Player? = null
    /**
     * If true, this item is not a special item anymore,
     * keeping only the snapshot of its current states.
     */
    var frozen: Boolean = false
        protected set
    /**
     * Make this instance not a special item anymore.
     */
    open fun froze(): ItemStack {
        if (frozen) {
            return this
        }
        frozen = true
        val nbt = NBTItem(this)
        nbt.removeKey(KEY_SIID)
        return nbt.item
    }

    /**
     * To make this item feel like it
     */
    abstract fun updateMeta(getter: Language.LangGetter)

    companion object {
        const val KEY_SIID = "si_id"

        fun isSpecial(item: ItemStack): Boolean {
            val nbt = NBTItem(item)
            return nbt.hasKey(StatefulSpecialItem.KEY_INSTANCE_ID) || nbt.hasKey(KEY_SIID)
        }

        operator fun get(item: ItemStack): SpecialItem? = StatelessSpecialItem[item] ?: StatefulSpecialItem[item]
    }
}