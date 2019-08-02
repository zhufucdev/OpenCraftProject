package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class Coin(amount: Int, getter: Language.LangGetter) : SpecialItem(Material.GOLD_INGOT, getter) {
    override val type: Type
        get() = Type.Coin

    init {
        updateItemMeta<ItemMeta> {
            setDisplayName(getter["coin.name"].toInfoMessage())
            lore = listOf(getter["coin.title"].toTipMessage())
            isUnbreakable = true
            addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS)
        }
        addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
        setAmount(amount)
    }

    override fun getSerialize(): ConfigurationSection {
        val r = super.getSerialize()
        if (amount > 1)
            r["amount"] = amount
        return r
    }

    companion object : SISerializable {
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
            config.getString("type") == Type.Coin.name
    }
}