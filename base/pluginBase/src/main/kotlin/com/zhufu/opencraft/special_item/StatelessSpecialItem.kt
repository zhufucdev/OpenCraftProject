package com.zhufu.opencraft.special_item

import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.reflections.Reflections
import java.util.*
import kotlin.reflect.full.companionObjectInstance

abstract class StatelessSpecialItem(m: Material, id: UUID) : SpecialItem(m, id) {
    companion object {
        val prebuilt = Reflections("com.zhufu.opencraft.special_item")
            .getSubTypesOf(StatelessSpecialItem::class.java)

        operator fun get(itemStack: ItemStack): StatelessSpecialItem? {
            if (itemStack.type == Material.AIR) return null
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
}