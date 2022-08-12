package com.zhufu.opencraft.special_item

import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.reflections.Reflections
import java.util.*
import kotlin.reflect.full.companionObjectInstance

abstract class StatelessSpecialItem(m: Material, id: UUID) : ItemStack(m) {
    companion object {
        const val KEY_SIID = "si_id"
        val prebuilt = Reflections("com.zhufu.opencraft.special_item")
            .getSubTypesOf(StatelessSpecialItem::class.java)
            .filter { it.superclass == StatelessSpecialItem::class.java }

        operator fun get(itemStack: ItemStack): StatelessSpecialItem? {
            try {
                val nbt = NBTItem(itemStack)
                val typeID = nbt.getUUID(KEY_SIID)
                prebuilt.forEach {
                    val companion = it.kotlin.companionObjectInstance
                    if (companion !is StatelessSICompanion) {
                        return@forEach
                    }
                    if (companion.SIID == typeID) {
                        return companion.fromItemStack(itemStack)
                    }
                }
            } catch (_: Exception) {
            }
            return null
        }
    }

    var inventoryPosition: Int = -1

    /**
     * If true, this item is not a special item anymore,
     * keep only the snapshot of its current states.
     */
    var frozen = false
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

    init {
        val nbt = NBTItem(this, true)
        nbt.setUUID(KEY_SIID, id)
    }
}