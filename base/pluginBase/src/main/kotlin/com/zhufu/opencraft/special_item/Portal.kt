package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.util.*
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class Portal : SpecialItem {

    fun setCount(amount: Int) {
        this.amount = amount
    }

    constructor(getter: Language.LangGetter, secondary: Boolean) : super(Material.OBSIDIAN, getter) {
        itemMeta = itemMeta!!.apply {
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

    constructor(getter: Language.LangGetter, itemStack: ItemStack) : this(getter) {
        amount = itemStack.amount
    }

    constructor(getter: Language.LangGetter): this(getter, false)

    override fun getSerialized(): ConfigurationSection {
        return super.getSerialized().apply { if (amount > 1) set("amount", amount) }
    }

    companion object : SISerializable {
        override fun deserialize(itemStack: ItemStack, getter: Language.LangGetter): SpecialItem =
            Portal(getter, itemStack)

        const val PRICE = 100
        private val displayNames: List<String> by lazy {
            Language.languages.map {
                it.getString("portal.name")!!
            }
        }

        override fun deserialize(config: ConfigurationSection, getter: Language.LangGetter): SpecialItem {
            if (isThis(config)) {
                return Portal(getter).apply {
                    if (config.isSet("amount")) {
                        amount = config.getInt("amount")
                    }
                }
            }
            throw IllegalArgumentException("Not a portal item.")
        }

        override fun isThis(itemStack: ItemStack?) =
            itemStack != null && itemStack.hasItemMeta()
                    && itemStack.itemMeta.displayName()
                ?.let { it is TextComponent && displayNames.contains(it.content()) } == true
                    && itemStack.itemMeta!!.hasItemFlag(ItemFlag.HIDE_ENCHANTS)

        override fun isThis(config: ConfigurationSection): Boolean =
            config.isSet("type") && config.getString("type") == "Portal"
    }
}