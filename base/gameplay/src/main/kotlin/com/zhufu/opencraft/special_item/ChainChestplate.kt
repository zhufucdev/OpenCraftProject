package com.zhufu.opencraft.special_item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

class ChainChestplate: Chestplate() {
    override val material: Material
        get() = Material.CHAINMAIL_CHESTPLATE

    override fun exp(enchant: Enchantment, level: Int): Int = when (enchant) {
        Enchantment.PROTECTION_ENVIRONMENTAL -> 60 * level
        Enchantment.PROTECTION_FIRE -> 55 * level
        Enchantment.THORNS -> 140 * level
        else -> 80 * level
    }
}