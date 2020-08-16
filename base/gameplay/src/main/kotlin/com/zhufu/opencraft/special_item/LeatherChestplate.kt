package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class LeatherChestplate : Chestplate() {
    override val material: Material
        get() = Material.LEATHER_CHESTPLATE

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 25 * level
        Enchantment.PROTECTION_FIRE -> 20 * level
        Enchantment.THORNS -> 100 * level
        else -> 30 * level
    }
    
}