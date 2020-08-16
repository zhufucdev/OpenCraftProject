package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class LeatherHelmet: Helmet() {
    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 20 * level
        Enchantment.DIG_SPEED -> 15 * level
        Enchantment.OXYGEN -> 15 * level
        else -> 25 * level
    }

    override val material: Material
        get() = Material.LEATHER_HELMET
}