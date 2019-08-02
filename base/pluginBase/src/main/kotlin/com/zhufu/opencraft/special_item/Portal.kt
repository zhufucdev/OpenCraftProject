package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.Language
import com.zhufu.opencraft.TextUtil
import com.zhufu.opencraft.TextUtil.TextColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class Portal : SpecialItem {
    override val type: Type
        get() = Type.Portal
    fun setCount(amount: Int){
        this.amount = amount
    }
    constructor(getter: Language.LangGetter,secondary: Boolean = false): super(Material.OBSIDIAN, getter) {
        itemMeta = itemMeta!!.apply {
            setDisplayName(TextUtil.getColoredText(getter["portal.name"] + if (secondary) " 2" else "", TextColor.LIGHT_PURPLE))

            val newLore = ArrayList<String>()
            TextUtil.formatLore(getter["portal.title"]).forEach {
                newLore.add(TextUtil.info(it))
            }
            TextUtil.formatLore(getter["portal.${if (!secondary) "subtitle" else "place"}"]).forEach {
                newLore.add(TextUtil.tip(it))
            }
            lore = newLore

            isUnbreakable = true
            addItemFlags(ItemFlag.HIDE_ENCHANTS)
            addEnchant(Enchantment.DURABILITY, 1, true)
        }
    }
    constructor(getter: Language.LangGetter,itemStack: ItemStack): this(getter){
        amount = itemStack.amount
    }

    override fun getSerialize(): ConfigurationSection {
        return super.getSerialize().apply { if (amount > 1) set("amount",amount) }
    }

    companion object : SISerializable {
        const val PRICE = 100
        var displayNames: List<String> private set

        init {
            val r = ArrayList<String>()
            Language.languages.forEach {
                r.add(
                    TextUtil.getColoredText(it.getString("portal.name")!!, TextColor.LIGHT_PURPLE)
                )
            }
            displayNames = r
        }

        override fun deserialize(config: ConfigurationSection, getter: Language.LangGetter): SpecialItem {
            if (isThis(config)) {
                return Portal(getter).apply {
                    if (config.isSet("amount")){
                        amount = config.getInt("amount")
                    }
                }
            }
            throw IllegalArgumentException("Not a portal item.")
        }

        override fun isThis(itemStack: ItemStack?) =
            itemStack != null && itemStack.hasItemMeta() && displayNames.any { itemStack.itemMeta!!.displayName.contains(it) } && itemStack.itemMeta!!.hasItemFlag(
                ItemFlag.HIDE_ENCHANTS
            )

        override fun isThis(config: ConfigurationSection): Boolean =
            config.isSet("type") && config.getString("type") == "Portal"
    }
}