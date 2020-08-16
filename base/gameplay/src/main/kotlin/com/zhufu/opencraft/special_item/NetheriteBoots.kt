package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class NetheriteBoots : Boots() {
    override val material: Material
        get() = Material.NETHERITE_BOOTS

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 240 * level
        Enchantment.PROTECTION_FALL -> 220 * level
        Enchantment.DURABILITY -> 110 * level * level
        else -> 260
    }
}