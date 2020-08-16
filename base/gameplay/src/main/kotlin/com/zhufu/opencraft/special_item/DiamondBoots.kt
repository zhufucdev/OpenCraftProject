package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class DiamondBoots : Boots() {
    override val material: Material
        get() = Material.DIAMOND_BOOTS

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 200 * level
        Enchantment.PROTECTION_FALL -> 170 * level
        Enchantment.DURABILITY -> 90 * level * level
        else -> 240
    }
}