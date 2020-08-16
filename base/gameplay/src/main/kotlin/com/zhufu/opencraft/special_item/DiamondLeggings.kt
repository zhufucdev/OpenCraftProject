package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class DiamondLeggings : Leggings() {
    override val material: Material
        get() = Material.DIAMOND_LEGGINGS

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.DURABILITY -> 90 * level * level
        else -> 200 * level
    }
}