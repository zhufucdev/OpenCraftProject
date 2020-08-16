package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.special_item.base.BindItem
import com.zhufu.opencraft.updateItemMeta
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.meta.ItemMeta

abstract class MultiDirectionEnchantEquipment: BindItem() {
    private val map = hashMapOf<Enchantment, Int>()

    operator fun get(enchant: Enchantment) = map.getOrDefault(enchant, 0)
    operator fun set(enchant: Enchantment, level: Int) {
        if (level > maxLevel(enchant)) throw IllegalArgumentException("The maximum level of ${enchant.key.key} is " +
                "${maxLevel(enchant)}.")
        map[enchant] = level
        itemLocation.itemStack.updateItemMeta<ItemMeta> {
            addEnchant(enchant, level, true)
        }
        itemLocation.push()
    }

    /**
     * The experience to upgrade the give [enchant] to a given [level].
     */
    abstract fun exp(enchant: Enchantment, level: Int): Int

    /**
     * Gets the maximum level of the given type of [enchant].
     */
    open fun maxLevel(enchant: Enchantment): Int = 4

    /**
     * A list of enchantments that can be upgraded in this item.
     */
    abstract val upgradable: List<Enchantment>
}