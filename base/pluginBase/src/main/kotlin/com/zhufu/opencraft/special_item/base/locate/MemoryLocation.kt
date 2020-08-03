package com.zhufu.opencraft.special_item.base.locate

import org.bukkit.inventory.ItemStack

class MemoryLocation(override var itemStack: ItemStack): ItemLocation() {
    override fun toString(): String = itemStack.toString()
    override fun serialize(): MutableMap<String, Any> = itemStack.serialize()
    override val isAvailable: Boolean
        get() = true
    companion object {
        @JvmStatic
        fun deserialize(d: Map<String, Any>) = MemoryLocation(ItemStack.deserialize(d))
    }
}