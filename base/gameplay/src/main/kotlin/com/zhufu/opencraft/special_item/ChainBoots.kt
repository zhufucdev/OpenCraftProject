package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class ChainBoots : Boots() {
    override val material: Material
        get() = Material.CHAINMAIL_BOOTS

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 30 * level
        Enchantment.PROTECTION_FALL -> 27 * level
        Enchantment.DURABILITY -> 80 * level
        else -> 120
    }
}