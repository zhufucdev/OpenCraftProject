package com.zhufu.opencraft

import com.zhufu.opencraft.special_item.*
import com.zhufu.opencraft.special_item.dynamic.SpecialItem
import com.zhufu.opencraft.special_item.static.WrappedItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.text.DecimalFormat

class GeneralPrise(val value: Long) : Prise<GeneralPrise>() {
    override fun compareTo(other: Prise<*>): Int = when (other) {
        is GeneralPrise -> value.compareTo(other.value)
        else -> compareTo(other.toGeneralPrise())
    }

    override fun plus(other: Prise<*>): Prise<GeneralPrise> = GeneralPrise(value + other.toGeneralPrise().value)

    override fun times(x: Int): GeneralPrise {
        return GeneralPrise(value * x)
    }

    override fun toGeneralPrise(): GeneralPrise = this

    override fun cost(player: Player): CostResult {
        val all = WrappedItem[player].filterIsInstance<GeneralCurrency>()
        var sum = 0L
        all.forEach { sum += it.value.toGeneralPrise().value }
        if (sum < value) {
            return CostFailure()
        }
        sum = 0L
        for (i in all) {
            if (value - sum < i.value.toGeneralPrise().value) {
                val x = value - sum
                val m = (x % i.unitValue.toGeneralPrise().value).toInt()
                i.amount -= (x / i.unitValue.value).toInt()
                if (m != 0) {
                    i.amount -= 1
                    player.inventory.addItem(
                        WrappedItem.make(
                            CopperCoin::class.simpleName!!,
                            (i.unitValue.value - m).toInt(),
                            player
                        )!!
                    )
                }

            }
        }
    }

    override fun cost(info: ServerPlayer): CostResult {
        if (info.currency < value)
            return CostFailure()
        info.currency -= value
        return object : CostResult {
            override val successful: Boolean
                get() = true

            override fun undo() {
                info.currency += value
            }
        }
    }

    override fun toString(getter: Language.LangGetter): String =
        getter["trade.coin.cost", DecimalFormat("#.##").format(value / 100.0)]

    companion object {
        fun of(item: ItemStack, owner: Player? = null): Prise<*> {
            val si = SpecialItem.getByItem(item, owner)
            if (si != null && si is CurrencyItem<*>) {
                return si.value
            } else {
                fun asPotion(): Long {

                }
                return GeneralPrise(
                    when (item.type) {
                        Material.BIRCH_PLANKS, Material.ACACIA_PLANKS, Material.CRIMSON_PLANKS,
                        Material.DARK_OAK_PLANKS, Material.JUNGLE_PLANKS, Material.OAK_PLANKS,
                        Material.SPRUCE_PLANKS, Material.WARPED_PLANKS -> 2
                        Material.IRON_INGOT -> 100
                        Material.LEATHER -> 5
                        Material.NETHERITE_INGOT -> 5000
                        Material.COBBLESTONE -> 2
                        Material.REDSTONE_TORCH, Material.REDSTONE -> 4
                        Material.REPEATER, Material.COMPARATOR, Material.REDSTONE_LAMP, Material.DAYLIGHT_DETECTOR -> 10
                        Material.LEVER -> 3
                        Material.STONE_BUTTON -> 2
                        Material.PISTON -> 20
                        Material.STICKY_PISTON -> 25
                        Material.IRON_PICKAXE -> 400
                        Material.IRON_AXE -> 500
                        Material.IRON_HOE -> 300
                        Material.IRON_SHOVEL -> 200
                        Material.POTION -> asPotion()
                    }
                )
            }
        }
    }
}