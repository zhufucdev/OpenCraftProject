package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class DiamondChestplate : Chestplate() {
    override val material: Material
        get() = Material.DIAMOND_CHESTPLATE

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 100 * level * level
        Enchantment.PROTECTION_FIRE -> 200 * level
        Enchantment.THORNS -> 110 * level * level
        else -> 115 * level * level
    }
}