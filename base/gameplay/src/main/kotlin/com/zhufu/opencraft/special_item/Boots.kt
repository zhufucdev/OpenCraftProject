package com.zhufu.opencraft.special_item

import org.bukkit.enchantments.Enchantment

abstract class Boots : MultiDirectionEnchantEquipment() {
    override val upgradable: List<Enchantment>
        get() = listOf(
            Enchantment.PROTECTION_ENVIRONMENTAL,
            Enchantment.PROTECTION_FALL,
            Enchantment.FROST_WALKER
        )

    override fun maxLevel(enchant: Enchantment): Int = if (enchant == Enchantment.FROST_WALKER) 1 else 4
}