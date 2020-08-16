package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class IronHelmet : Helmet() {
    override val material: Material
        get() = Material.IRON_HELMET

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 120 * level
        Enchantment.OXYGEN -> 70 * level
        Enchantment.DIG_SPEED -> 70 * level
        else -> 180 * level
    }
}