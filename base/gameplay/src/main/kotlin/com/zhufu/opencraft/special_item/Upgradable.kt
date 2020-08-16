package com.zhufu.opencraft.special_item

interface Upgradable {
    /**
     * The current level of this type of item.
     */
    var level: Int

    /**
     * The maximum of [level].
     */
    val maxLevel: Int

    /**
     * Called whenever the [level] changes.
     */
    fun updateDisplay()

    /**
     * The experience to upgrade to the given [level].
     */
    fun exp(level: Int): Int
}