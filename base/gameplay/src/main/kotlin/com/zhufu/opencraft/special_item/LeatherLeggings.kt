package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class LeatherLeggings : Leggings() {
    override val material: Material
        get() = Material.LEATHER_LEGGINGS

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 20 * level
        Enchantment.PROTECTION_FIRE -> 20 * level
        else -> 25 * level
    }
}