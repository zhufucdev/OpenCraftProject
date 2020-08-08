package com.zhufu.opencraft.special_item.base.locate

import org.bukkit.Bukkit
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import java.util.*

class DroppedItemLocation: ItemLocation {
    val id: UUID
    constructor(entity: Item): super() {
        id = entity.uniqueId
    }
    constructor(id: UUID): super() {
        this.id = id
    }

    val entity get() = Bukkit.getEntity(id) as Item
    override val itemStack: ItemStack by lazy { entity.itemStack }
    override val isAvailable: Boolean
        get() = Bukkit.getEntity(id) != null && !entity.isDead

    override fun push() {
        entity.itemStack = itemStack
    }

    override fun serialize(): MutableMap<String, Any> = mutableMapOf("entityID" to id.toString())

    companion object {
        @JvmStatic
        fun deserialize(d: Map<String, Any>) =
            DroppedItemLocation(UUID.fromString(d["entityID"] as String))
    }
}