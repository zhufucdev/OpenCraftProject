package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.updateItemMeta
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.meta.ItemMeta

class StoneAxe : FighterSword() {
    override fun exp(level: Int): Int {
        return when (level) {
            2 -> 100
            3 -> 200
            4 -> 400
            else -> throw IllegalArgumentException("level must be between 2 and 4.")
        }
    }

    override val material: Material
        get() = Material.STONE_AXE

    override fun updateEnchantment() {
        if (level !in 1..4) {
            throw IllegalArgumentException("Level must be between 1 and 4.")
        }
        itemStack.updateItemMeta<ItemMeta> {
            when (level) {
                1 -> setEnchants(Enchantment.DAMAGE_ALL to 1)
                2 -> setEnchants(
                    Enchantment.DAMAGE_ALL to 2,
                    Enchantment.KNOCKBACK to 1
                )
                3 -> setEnchants(
                    Enchantment.DAMAGE_ALL to 2,
                    Enchantment.KNOCKBACK to 2,
                    Enchantment.FIRE_ASPECT to 1
                )
                else -> setEnchants(
                    Enchantment.DAMAGE_ALL to 3,
                    Enchantment.KNOCKBACK to 2,
                    Enchantment.FIRE_ASPECT to 2
                )
            }
        }
        itemLocation.push()
    }
}