package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class NetheriteHelmet : Helmet() {
    override val material: Material
        get() = Material.NETHERITE_HELMET

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 240 * level
        Enchantment.DURABILITY -> 300 * level
        else -> 120 * level
    }
}