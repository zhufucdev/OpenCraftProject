package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.updateItemMeta
import com.zhufu.opencraft.util.*
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

class Portal : StatelessSpecialItem {
    constructor(getter: Language.LangGetter, secondary: Boolean = false) : super(Material.OBSIDIAN, SIID) {
        updateItemMeta<ItemMeta> {
            displayName(
                (getter["portal.name"] + if (secondary) " 2" else "").toComponent().color(NamedTextColor.LIGHT_PURPLE)
            )

            lore(buildList {
                TextUtil.formatLore(getter["portal.title"]).forEach {
                    add(it.toInfoMessage())
                }
                TextUtil.formatLore(getter["portal.${if (!secondary) "subtitle" else "place"}"]).forEach {
                    add(it.toTipMessage())
                }
            })

            isUnbreakable = true
            addItemFlags(ItemFlag.HIDE_ENCHANTS)
            addEnchant(Enchantment.DURABILITY, 1, true)
        }
    }

    constructor(from: ItemStack) : super(Material.OBSIDIAN, SIID) {
        this.amount = from.amount
        this.itemMeta = from.itemMeta
    }

    companion object : StatelessSICompanion {
        const val PRICE = 100
        override val SIID: UUID get() = UUID.fromString("F107A402-09D3-4D9D-B5CA-D05F12876324")
        override fun newInstance(getter: Language.LangGetter, madeFor: Player) = Portal(getter)
        override fun fromItemStack(item: ItemStack) = Portal(item)
    }
}