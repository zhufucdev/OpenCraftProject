package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.util.Language
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

interface SISerializable {
    fun deserialize(config: ConfigurationSection, getter: Language.LangGetter): SpecialItem
    fun deserialize(itemStack: ItemStack, getter: Language.LangGetter): SpecialItem
    fun isThis(itemStack: ItemStack?): Boolean
    fun isThis(config: ConfigurationSection): Boolean
}