package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class DiamondHelmet : Helmet() {
    override val material: Material
        get() = Material.DIAMOND_HELMET

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 200 * level
        Enchantment.OXYGEN -> 120 * level
        Enchantment.DIG_SPEED -> 120 * level
        else -> 250 * level
    }
}