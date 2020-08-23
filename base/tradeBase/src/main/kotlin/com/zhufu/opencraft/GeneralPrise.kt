package com.zhufu.opencraft

import com.zhufu.opencraft.special_item.*
import com.zhufu.opencraft.special_item.dynamic.SpecialItem
import com.zhufu.opencraft.special_item.static.WrappedItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.SpectralArrow
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.text.DecimalFormat
import kotlin.reflect.KClass

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
        val all = WrappedItem[player].filterIsInstance<GeneralCurrency>().sortedBy { it.unitValue }
        var sum = 0L
        all.forEach { sum += it.value.toGeneralPrise().value }
        if (sum < value) {
            return costFailure
        }
        sum = 0L
        val lost = hashMapOf<String, Int>()
        fun lose(clazz: KClass<*>, amount: Int) {
            val name = clazz.qualifiedName ?: clazz.simpleName!!
            lost[name] = lost.getOrDefault(name, 0) + amount
        }

        fun lose(item: GeneralCurrency, amount: Int) {
            lose(item::class, amount)
        }
        for (i in all) {
            if ((value - sum).toGP() < i.value) {
                val x = value - sum
                val m = (x % i.unitValue.toGeneralPrise().value).toInt()
                i.amount -= (x / i.unitValue.value).toInt().also { lose(i, it) }
                if (m != 0) {
                    i.amount -= 1
                    lose(i, 1)
                    player.inventory.addItem(
                        WrappedItem.make(
                            CopperCoin::class.simpleName!!,
                            (i.unitValue.value - m).toInt().also {
                                lose(CopperCoin::class, -it)
                            },
                            player
                        )!!
                    )
                }
                i.push()
                break
            } else {
                sum += i.value.toGeneralPrise().value
                lose(i, i.amount)
                i.amount = 0
                if (sum >= value) {
                    break
                }
            }
        }
        return object : CostResult {
            override val successful: Boolean
                get() = true

            override fun undo() {
                lost.forEach { (t, u) ->
                    player.setInventory(WrappedItem.make(t, 1, player)!!, -u)
                }
            }
        }
    }

    override fun cost(info: ServerPlayer): CostResult {
        if (info.currency < value)
            return costFailure
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
}