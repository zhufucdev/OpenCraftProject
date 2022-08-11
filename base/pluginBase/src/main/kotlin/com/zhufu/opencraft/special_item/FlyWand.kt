package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.PlayerModifier
import com.zhufu.opencraft.updateItemMeta
import com.zhufu.opencraft.util.*
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.scoreboard.Objective
import kotlin.math.round

class FlyWand : SpecialItem {
    constructor(getter: Language.LangGetter, initializeTime: Boolean) : super(Material.STICK, getter) {
        updateItemMeta<ItemMeta> {
            displayName(getter["wand.name"].toComponent().color(NamedTextColor.RED))
            isUnbreakable = true
            addItemFlags(ItemFlag.HIDE_ENCHANTS)
            addEnchant(Enchantment.DURABILITY, 1, true)
        }
        if (initializeTime)
            updateTime(MAX_TIME_REMAINING)
    }

    constructor(itemStack: ItemStack, getter: Language.LangGetter) : this(getter, false) {
        val lore = (itemStack.itemMeta!!.lore()!![1] as TextComponent).content()
        val first = lore.indexOfFirst { it.isDigit() || it == '-' }
        val last = lore.indexOfLast { it.isDigit() }
        if (first == -1 || last == -1) {
            this.timeRemaining = 0.0
        } else {
            val num = lore.substring(first, last + 1)
            this.timeRemaining = num.toDoubleOrNull() ?: 0.0
        }
    }

    constructor(timeRemaining: Double, getter: Language.LangGetter) : this(getter, false) {
        updateTime(timeRemaining)
    }

    constructor(getter: Language.LangGetter) : this(getter, true)

    override fun doPerTick(mod: PlayerModifier, data: YamlConfiguration, score: Objective, scoreboardSorter: Int) {
        if (!data.isSet("hasFlyWand")) {
            var allowFlight =
                mod.player.gameMode == GameMode.CREATIVE || mod.player.gameMode == GameMode.SPECTATOR
            data.set("hasFlyWand", true)
            if (!allowFlight) {
                if (mod.player.isFlying) {
                    updateTime(timeRemaining - 0.05)
                    if (inventoryPosition != -1)
                        mod.player.inventory.setItem(inventoryPosition, this)
                }
                if (!isUpToTime) {
                    allowFlight = true
                }
                score.getScore(
                    TextUtil.getColoredText(
                        getter["server.statics.flyRemaining", round(timeRemaining)],
                        TextUtil.TextColor.RED
                    )
                ).score = scoreboardSorter
                mod.isFlyable = allowFlight
            }
        }
    }

    companion object : SISerializable {
        const val MAX_TIME_REMAINING = 30 * 60.0
        const val PRICE_PER_MIN = 100
        private val displayNames: List<String> by lazy {
            Language.languages.map {
                it.getString("wand.name")!!
            }
        }

        override fun deserialize(itemStack: ItemStack, getter: Language.LangGetter): SpecialItem =
            FlyWand(itemStack, getter)

        override fun deserialize(config: ConfigurationSection, getter: Language.LangGetter): FlyWand {
            if (config.isSet("timeRemaining")) {
                return FlyWand(
                    config.getDouble(
                        "timeRemaining"
                    ),
                    getter
                )
            }
            return FlyWand(MAX_TIME_REMAINING, getter)
        }

        override fun isThis(itemStack: ItemStack?): Boolean {
            return (itemStack != null
                    && itemStack.hasItemMeta()
                    && itemStack.itemMeta.displayName()
                ?.let { it is TextComponent && displayNames.contains(it.content()) } == true
                    && itemStack.itemMeta!!.isUnbreakable)
        }

        override fun isThis(config: ConfigurationSection): Boolean {
            return config["type"] == FlyWand::class.simpleName
        }
    }

    var timeRemaining = MAX_TIME_REMAINING
        private set

    fun updateTime(timeRemaining: Double) {
        this.timeRemaining = timeRemaining
        itemMeta = itemMeta!!.apply {
            lore(
                listOf(
                    getter["wand.title"].toTipMessage(),
                    getter["wand.subtitle", timeRemaining].toInfoMessage()
                )
            )
        }
    }

    val isUpToTime: Boolean
        get() = timeRemaining <= 0

    override fun getSerialized(): ConfigurationSection {
        val config = super.getSerialized()
        if (timeRemaining != MAX_TIME_REMAINING)
            config["timeRemaining"] = timeRemaining
        return config
    }

    override fun clone(): FlyWand {
        return FlyWand(this.timeRemaining, getter)
    }
}