package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class ChainLeggings : Leggings() {
    override val material: Material
        get() = Material.CHAINMAIL_LEGGINGS

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 50 * level
        Enchantment.PROTECTION_FIRE -> 50 * level
        else -> 80 * level
    }
}