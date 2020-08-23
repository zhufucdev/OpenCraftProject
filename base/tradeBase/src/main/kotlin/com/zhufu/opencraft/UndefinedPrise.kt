package com.zhufu.opencraft

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt

class UndefinedPrise(private val value: Int) : Prise<UndefinedPrise>() {
    override fun compareTo(other: Prise<*>): Int = when (other) {
        is UndefinedPrise -> value.compareTo(other.value)
        else -> toGeneralPrise().compareTo(other.toGeneralPrise())
    }

    override fun plus(other: Prise<*>): Prise<UndefinedPrise> = UndefinedPrise(
        value + when (other) {
            is UndefinedPrise -> other.value
            is GeneralPrise -> (other.value / 200.0).roundToInt()
            else -> throw UnsupportedOperationException("Adding a ${other::class.simpleName} to a UndefinedPrise.")
        }
    )

    override fun times(x: Int): Prise<UndefinedPrise> = UndefinedPrise(value * x)

    override fun toGeneralPrise(): GeneralPrise = GeneralPrise(100L * value)

    override fun cost(player: Player): CostResult {
        val items = TreeMap<ItemStack, Int> { o1, o2 -> of(o1.type).compareTo(of(o2.type)) }
        player.inventory.forEachIndexed { index, itemStack ->
            if (of(itemStack?.type ?: return@forEachIndexed).value > 0)
             items[itemStack] = index
        }
        val sum = items.keys.sumBy { of(it.type).value * it.amount }
        if (sum < value) {
            return costFailure
        }

        val lost = hashMapOf<Material, Int>()
        fun lose(material: Material, amount: Int) {
            lost[material] = lost.getOrDefault(material, 0) + amount
        }
        var cost = 0
        for((it, index) in items) {
            val unit = of(it.type).value
            if (value - cost < unit) {
                val target = value - cost
                val x = target - target % unit
                it.amount -= (target / unit + 1).also { a -> lose(it.type, a) }
                player.inventory.setItem(index, it)
                player.setInventory(ItemStack(Material.PRISMARINE_SHARD), x)
                lose(Material.PRISMARINE_SHARD, -x)
                break
            } else {
                val amount = (value - cost) / unit
                it.amount -= amount
                lose(it.type, amount)
                player.inventory.setItem(index, it)
                cost += amount * unit
            }
        }

        return object : CostResult {
            override val successful: Boolean
                get() = true

            override fun undo() {
                lost.forEach { t, u -> player.setInventory(ItemStack(t), u) }
            }
        }
    }

    override fun cost(info: ServerPlayer): CostResult = costFailure

    override fun toString(getter: Language.LangGetter): String = getter["trade.undefined.cost", value]

    companion object {
        fun of(material: Material) = when (material) {
            Material.PRISMARINE_SHARD -> 1
            Material.DIAMOND -> 3
            Material.EMERALD -> 4
            Material.NETHER_STAR -> 5
            else -> 0
        }.toUP()
    }
}