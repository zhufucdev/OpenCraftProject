package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class LeatherBoots : Boots() {
    override val material: Material
        get() = Material.LEATHER_BOOTS

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 20 * level
        Enchantment.PROTECTION_FALL -> 20 * level
        Enchantment.DURABILITY -> 25 * level
        else -> 50
    }
}