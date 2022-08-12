package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.PlayerModifier
import com.zhufu.opencraft.updateItemMeta
import com.zhufu.opencraft.util.*
import de.tr7zw.nbtapi.NBTCompound
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.scoreboard.Objective
import java.util.UUID
import kotlin.math.round
import kotlin.math.roundToInt

class FlyWand(getter: Language.LangGetter, timeRemaining: Double = MAX_TIME_REMAINING, id: UUID = UUID.randomUUID()) :
    StatefulSpecialItem(Material.STICK, getter, id, SIID) {

    override fun tick(mod: PlayerModifier, data: YamlConfiguration, score: Objective, scoreboardSorter: Int) {
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

    companion object : StatefulSICompanion {
        const val MAX_TIME_REMAINING = 30 * 60.0
        const val PRICE_PER_MIN = 100

        override fun deserialize(specialItemID: UUID, nbt: NBTCompound, getter: Language.LangGetter): FlyWand {
            return FlyWand(
                getter,
                nbt.getDouble(
                    "remaining"
                ),
                specialItemID
            )
        }

        override fun newInstance(getter: Language.LangGetter, madeFor: Player): StatefulSpecialItem = FlyWand(getter)

        override val SIID: UUID
            get() = UUID.fromString("7EEC1DBD-86CE-45A4-9FE3-697CEFB5CDCB")
    }

    var timeRemaining: Double
        get() = nbt.getDouble("remaining")
        private set(value) {
            nbt.setDouble("remaining", value)
        }

    fun updateTime(timeRemaining: Double) {
        this.timeRemaining = timeRemaining
        updateItemMeta<ItemMeta> {
            displayName(getter["wand.name"].toComponent().color(NamedTextColor.RED))
            isUnbreakable = true
            addItemFlags(ItemFlag.HIDE_ENCHANTS)
            addEnchant(Enchantment.DURABILITY, 1, true)
            lore(
                listOf(
                    getter["wand.title"].toTipMessage(),
                    getter["wand.subtitle", timeRemaining.roundToInt()].toInfoMessage()
                )
            )
        }
    }

    val isUpToTime: Boolean
        get() = timeRemaining <= 0

    init {
        updateTime(timeRemaining)
    }
}