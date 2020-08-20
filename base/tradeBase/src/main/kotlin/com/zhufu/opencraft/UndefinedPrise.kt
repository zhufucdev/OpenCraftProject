package com.zhufu.opencraft

import org.bukkit.entity.Player
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

    override fun toGeneralPrise(): GeneralPrise = GeneralPrise(200L * value)

    override fun cost(player: Player): CostResult {
        TODO("Not yet implemented")
    }

    override fun cost(info: ServerPlayer): CostResult {
        TODO("Not yet implemented")
    }

    override fun toString(getter: Language.LangGetter): String {
        TODO("Not yet implemented")
    }


}