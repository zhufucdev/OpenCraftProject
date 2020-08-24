package com.zhufu.opencraft

import com.zhufu.opencraft.special_item.*
import com.zhufu.opencraft.special_item.static.WrappedItem
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.text.DecimalFormat
import kotlin.reflect.KClass

class GeneralPrise(val value: Long) : Prise<GeneralPrise>() {
    override fun compareTo(other: Prise<*>): Int = if (other is UndefinedPrise) {
        -1
    } else {
        super.compareTo(other)
    }

    override fun plus(other: Prise<*>): Prise<GeneralPrise> = GeneralPrise(value + other.toGeneralPrise().value)

    override fun times(x: Int): GeneralPrise = GeneralPrise(value * x)
    override fun div(x: Int): Prise<GeneralPrise> = GeneralPrise(value / x)
    override fun div(other: Prise<*>): Int = (value / other.toGeneralPrise().value).toInt()
    override fun rem(other: Prise<*>): Prise<GeneralPrise> = GeneralPrise(value % other.toGeneralPrise().value)
    override fun isZero(): Boolean = value == 0L

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
        getter["trade.coin.cost", DecimalFormat("#.##").format(value / 10.0)]

    @Suppress("UNCHECKED_CAST")
    override fun generateItem(owner: Player): List<ItemStack> {
        var remaining = value
        val r = arrayListOf<ItemStack>()
        while (remaining > 0) {
            listOf(Silver::class.simpleName, Gold::class.simpleName, Coin::class.simpleName, CopperCoin::class.simpleName)
                .forEach {
                    val item = WrappedItem.make(it!!, 1, owner) as CurrencyItem<GeneralPrise>
                    if (remaining >= item.unitValue.value) {
                        val amount = remaining / item.unitValue.value
                        remaining -= amount * item.unitValue.value
                        val stacks = amount / 64
                        for (i in 0 until stacks) {
                            r.add(item.apply { this.amount = 64 })
                        }
                        r.add(item.apply { this.amount = (amount % 64).toInt() })
                    }
                }
        }

        return r
    }
}