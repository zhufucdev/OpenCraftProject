package com.zhufu.opencraft

import org.bukkit.entity.Player

abstract class Prise<T> : Comparable<Prise<*>> {
    /**
     * Remove the items of the amount of currency of this object from the given [player]'s inventory.
     * @return true if this removal is successful.
     */
    abstract fun cost(player: Player): CostResult

    /**
     * Remove the amount of currency of this object from the given [info]'s deposit.
     */
    abstract fun cost(info: ServerPlayer): CostResult

    /**
     * Return a new [Prise] whose value is [x] times of this.
     */
    abstract operator fun times(x: Int): Prise<T>

    /**
     * Return a new [Prise] whose value is worth [other] more than this.
     */
    abstract operator fun plus(other: Prise<*>): Prise<T>
    operator fun minus(other: Prise<*>): Prise<T> = plus(other * -1)

    /**
     * Return a [GeneralPrise] instance whose value is the same as this.
     */
    abstract fun toGeneralPrise(): GeneralPrise

    abstract fun toString(getter: Language.LangGetter): String

    interface CostResult {
        val successful: Boolean
        fun undo()
    }

    class CostFailure : CostResult {
        override val successful: Boolean
            get() = false

        override fun undo() {
        }
    }
}