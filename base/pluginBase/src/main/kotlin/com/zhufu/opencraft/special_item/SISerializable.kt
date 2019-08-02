package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.Language
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

interface SISerializable {
    fun deserialize(config: ConfigurationSection,getter: Language.LangGetter): SpecialItem
    fun isThis(itemStack: ItemStack?): Boolean
    fun isThis(config: ConfigurationSection): Boolean
}