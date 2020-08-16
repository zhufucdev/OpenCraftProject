package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class IronLeggings : Leggings() {
    override val material: Material
        get() = Material.IRON_LEGGINGS

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.DURABILITY -> 190 * level
        else -> 120 * level
    }
}