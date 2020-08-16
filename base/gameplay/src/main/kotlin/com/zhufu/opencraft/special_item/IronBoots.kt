package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class IronBoots : Boots() {
    override val material: Material
        get() = Material.IRON_BOOTS

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 120 * level
        Enchantment.PROTECTION_FALL -> 110 * level
        Enchantment.DURABILITY -> 190 * level
        else -> 200
    }
}