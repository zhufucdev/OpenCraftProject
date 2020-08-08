package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.updateItemMeta
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.meta.ItemMeta

class NetheriteAxe: FighterSword() {
    override fun exp(level: Int): Int {
        return when (level) {
            2 -> 1200
            3 -> 2000
            4 -> 2800
            else -> throw IllegalArgumentException("level must be between 2 and 4.")
        }
    }

    override val material: Material
        get() = Material.NETHERITE_AXE

    override fun updateEnchantment() {
        if (level !in 1..4) {
            throw IllegalArgumentException("Level must be between 1 and 4.")
        }
        itemStack.updateItemMeta<ItemMeta> {
            when (level) {
                1 -> setEnchants(
                    Enchantment.DAMAGE_ALL to 2,
                    Enchantment.KNOCKBACK to 1,
                    Enchantment.FIRE_ASPECT to 1
                )
                2 -> setEnchants(
                    Enchantment.DAMAGE_ALL to 3,
                    Enchantment.KNOCKBACK to 2,
                    Enchantment.FIRE_ASPECT to 2
                )
                3 -> setEnchants(
                    Enchantment.DAMAGE_ALL to 4,
                    Enchantment.KNOCKBACK to 3,
                    Enchantment.FIRE_ASPECT to 3
                )
                else -> setEnchants(
                    Enchantment.DAMAGE_ALL to 5,
                    Enchantment.KNOCKBACK to 3,
                    Enchantment.FIRE_ASPECT to 3
                )
            }
        }
        itemLocation.push()
    }
}