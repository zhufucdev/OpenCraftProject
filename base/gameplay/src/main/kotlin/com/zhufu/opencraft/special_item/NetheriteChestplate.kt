package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class NetheriteChestplate : Chestplate() {
    override val material: Material
        get() = Material.NETHERITE_CHESTPLATE

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL ->120 * level * level
        Enchantment.PROTECTION_FIRE -> 250 * level
        Enchantment.THORNS -> 130 * level * level
        else -> 140 * level * level
    }
}