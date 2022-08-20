package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toTipMessage
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.UUID

class Coin : StatelessSpecialItem {
    constructor(getter: Language.LangGetter) : super(Material.GOLD_INGOT, SIID) {
        updateMeta(getter)
    }

    constructor(from: ItemStack) : super(Material.GOLD_INGOT, SIID) {
        this.amount = from.amount
        this.itemMeta = from.itemMeta
    }

    override fun updateMeta(getter: Language.LangGetter) {
        updateItemMeta<ItemMeta> {
            displayName(getter["coin.name"].toInfoMessage())
            lore(listOf(getter["coin.title"].toTipMessage()))
            isUnbreakable = true
            addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS)
        }
        addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
    }

    companion object : StatelessSICompanion {
        override val SIID: UUID get() = UUID.fromString("DD6F0169-0665-45E7-8343-0751669058DB")
        override fun newInstance(getter: Language.LangGetter, madeFor: Player) = Coin(getter)
        override fun fromItemStack(item: ItemStack) = Coin(item)
    }
}