package com.zhufu.opencraft.special_item

import org.bukkit.enchantments.Enchantment

abstract class Chestplate: MultiDirectionEnchantEquipment() {
    override val upgradable: List<Enchantment>
        get() = listOf(
            Enchantment.PROTECTION_ENVIRONMENTAL,
            Enchantment.PROTECTION_FIRE,
            Enchantment.THORNS,
            Enchantment.DURABILITY
        )
}