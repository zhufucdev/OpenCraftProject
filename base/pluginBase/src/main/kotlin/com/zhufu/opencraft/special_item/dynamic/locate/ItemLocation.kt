package com.zhufu.opencraft.special_item.dynamic.locate

import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.inventory.ItemStack

abstract class ItemLocation : ConfigurationSerializable {
    abstract val itemStack: ItemStack
    open fun push() {}
    abstract val isAvailable: Boolean
}