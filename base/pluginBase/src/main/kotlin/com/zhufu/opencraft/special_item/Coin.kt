package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class Coin : SpecialItem {
    constructor(amount: Int, getter: Language.LangGetter): super(Material.GOLD_INGOT, getter) {
        updateItemMeta<ItemMeta> {
            displayName(getter["coin.name"].toInfoMessage())
            lore(listOf(getter["coin.title"].toTipMessage()))
            isUnbreakable = true
            addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS)
        }
        addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
        setAmount(amount)
    }

    constructor(getter: Language.LangGetter): this(1, getter)

    override fun getSerialized(): ConfigurationSection {
        val r = super.getSerialized()
        if (amount > 1)
            r["amount"] = amount
        return r
    }

    companion object : SISerializable {
        override fun deserialize(itemStack: ItemStack, getter: Language.LangGetter) = Coin(itemStack.amount, getter)

        override fun deserialize(config: ConfigurationSection, getter: Language.LangGetter): SpecialItem =
            Coin(config.getInt("amount", 1), getter)

        override fun isThis(itemStack: ItemStack?): Boolean =
            itemStack != null
                    && with(itemStack) {
                type == Material.GOLD_INGOT
                        && hasItemMeta()
                        && itemMeta!!.hasItemFlag(ItemFlag.HIDE_UNBREAKABLE)
                        && itemMeta!!.hasItemFlag(ItemFlag.HIDE_ENCHANTS)
                        && itemMeta!!.isUnbreakable
            }

        override fun isThis(config: ConfigurationSection): Boolean =
            config.getString("type") == Coin::class.simpleName
    }
}