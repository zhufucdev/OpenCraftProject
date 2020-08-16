package com.zhufu.opencraft.special_item

import org.bukkit.enchantments.Enchantment

abstract class Helmet : MultiDirectionEnchantEquipment() {
    override val upgradable: List<Enchantment>
        get() = listOf(
            Enchantment.PROTECTION_ENVIRONMENTAL,
            Enchantment.DIG_SPEED,
            Enchantment.OXYGEN,
            Enchantment.DURABILITY
        )
}