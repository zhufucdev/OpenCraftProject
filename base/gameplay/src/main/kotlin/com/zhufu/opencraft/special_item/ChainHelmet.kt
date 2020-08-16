package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class ChainHelmet : Helmet() {
    override val material: Material
        get() = Material.CHAINMAIL_HELMET

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 50 * level
        Enchantment.OXYGEN -> 30 * level
        Enchantment.DIG_SPEED -> 30 * level
        else -> 70 * level
    }
}