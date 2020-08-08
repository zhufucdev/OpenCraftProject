package com.zhufu.opencraft.special_item.base.locate

import com.zhufu.opencraft.special_item.base.SpecialItem
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Chest
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.util.*

class InventoryLocation(val inventory: Inventory, val id: UUID) : ItemLocation() {
    override val itemStack: ItemStack
        get() = inventory.first { SpecialItem.getSIID(it) == id }
    override val isAvailable: Boolean
        get() = inventory.any { SpecialItem.getSIID(it) == id }

    override fun serialize(): MutableMap<String, Any> = mutableMapOf(
        "location" to (inventory.location
            ?: error("Virtual inventory cannot be serialized."))
            .let<Location, Any> {
                val block = it.block.state
                if (block == inventory.holder)
                    it
                else
                    it.getNearbyLivingEntities(1.0).first { e -> e == inventory.holder }.uniqueId.toString()
            },
        "id" to id.toString()
    )

    companion object {
        @JvmStatic
        fun deserialize(d: Map<String, Any>): InventoryLocation {
            return InventoryLocation(
                d["location"].let {
                    when (it) {
                        is String -> (Bukkit.getEntity(UUID.fromString(it)) as InventoryHolder?)?.inventory
                            ?: error("Entity inventory holder not found.")
                        is Location -> (it.block.state as InventoryHolder).inventory
                        else -> error("Invalid serialization: location.")
                    }
                },
                UUID.fromString(d["id"] as String)
            )
        }
    }
}