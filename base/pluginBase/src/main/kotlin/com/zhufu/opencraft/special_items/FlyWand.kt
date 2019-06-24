package com.zhufu.opencraft.special_items

import com.zhufu.opencraft.Language
import com.zhufu.opencraft.TextUtil
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class FlyWand : SpecialItem {
    private val getter: Language.LangGetter
    constructor(getter: Language.LangGetter, initializeTime: Boolean = true): super(Material.STICK){
        this.getter = getter
        itemMeta = itemMeta!!.apply {
            setDisplayName(TextUtil.getColoredText(getter["wand.name"], TextUtil.TextColor.RED))
            isUnbreakable = true
            addItemFlags(ItemFlag.HIDE_ENCHANTS)
            addEnchant(Enchantment.DURABILITY,1,true)
        }
        if (initializeTime)
            updateTime(MAX_TIME_REMAINING)
    }
    constructor(itemStack: ItemStack, getter: Language.LangGetter) : this(getter,false){
        val lore = itemStack.itemMeta!!.lore!![1]
        val first = lore.indexOfFirst { it.isDigit() || it == '-' }
        val last = lore.indexOfLast { it.isDigit() }
        if (first == -1 || last == -1) {
            this.timeRemaining = 0
        } else {
            val num = lore.substring(first,last+1)
            this.timeRemaining = num.toLongOrNull() ?: 0L
        }
    }
    constructor(timeRemaining: Long,getter: Language.LangGetter): this(getter,false){
        updateTime(timeRemaining)
    }

    companion object: SISerializable {
        const val MAX_TIME_REMAINING = 30*60L
        const val PRICE_PER_MIN = 100
        var displayNames: List<String> private set
        init {
            val r = ArrayList<String>()
            Language.languages.forEach {
                r.add(
                    TextUtil.getColoredText(it.getString("wand.name")!!, TextUtil.TextColor.RED)
                )
            }
            displayNames = r
        }

        override fun deserialize(config: ConfigurationSection, getter: Language.LangGetter): FlyWand {
            if (config.isSet("timeRemaining")){
                return FlyWand(
                    config.getLong(
                        "timeRemaining"
                    ),
                    getter
                )
            }
            return FlyWand(MAX_TIME_REMAINING,getter)
        }

        override fun isThis(itemStack: ItemStack?): Boolean {
            val r = itemStack != null && itemStack.hasItemMeta() && displayNames.contains(itemStack.itemMeta!!.displayName) && itemStack.itemMeta!!.isUnbreakable
            if (r && !itemStack!!.containsEnchantment(Enchantment.DURABILITY)){
                itemStack.itemMeta!!.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                itemStack.itemMeta!!.addEnchant(Enchantment.DURABILITY,1,true)
            }
            return r
        }
        override fun isThis(config: ConfigurationSection): Boolean{
            return config["type"] == Type.FlyingWand.name
        }
    }
    var timeRemaining = MAX_TIME_REMAINING
        private set
    fun updateTime(timeRemaining: Long){
        this.timeRemaining = timeRemaining
        itemMeta = itemMeta!!.apply {
            lore = listOf(
                TextUtil.tip(getter["wand.title"]),
                TextUtil.info(getter["wand.subtitle",timeRemaining])
            )
        }
    }
    val isUpToTime: Boolean
        get() = timeRemaining <= 0
    override val type: Type
        get() = Type.FlyingWand

    override fun getSerialize(): ConfigurationSection{
        val config = super.getSerialize()
        if (timeRemaining != MAX_TIME_REMAINING)
            config["timeRemaining"] = timeRemaining
        return config
    }

    override fun clone(): FlyWand {
        return FlyWand(this.timeRemaining,getter)
    }
}