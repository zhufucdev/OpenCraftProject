package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class IronChestplate : Chestplate() {
    override val material: Material
        get() = Material.IRON_CHESTPLATE

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 70 * level * level
        Enchantment.PROTECTION_FIRE -> 130 * level
        else -> 75 * level * level
    }
}