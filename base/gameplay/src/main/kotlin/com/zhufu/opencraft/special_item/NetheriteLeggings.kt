package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class NetheriteLeggings : Leggings() {
    override val material: Material
        get() = Material.NETHERITE_LEGGINGS

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.DURABILITY -> 110 * level * level
        else -> 240 * level
    }
}