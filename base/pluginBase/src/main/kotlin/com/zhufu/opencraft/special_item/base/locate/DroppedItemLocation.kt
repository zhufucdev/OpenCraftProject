package com.zhufu.opencraft.special_item.base.locate

import org.bukkit.Bukkit
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import java.util.*

class DroppedItemLocation(val entity: Item) : ItemLocation() {
    override val itemStack: ItemStack by lazy { entity.itemStack }
    override val isAvailable: Boolean
        get() = !entity.isDead

    override fun push() {
        entity.itemStack = itemStack
    }

    override fun serialize(): MutableMap<String, Any> = mutableMapOf("entityID" to entity.uniqueId.toString())

    companion object {
        @JvmStatic
        fun deserialize(d: Map<String, Any>) =
            DroppedItemLocation(Bukkit.getEntity(UUID.fromString(d["entityID"] as String)) as Item)
    }
}