package com.zhufu.opencraft.special_item

interface Upgradable {
    var level: Int
    val maxLevel: Int
    fun updateDisplay()
    fun exp(level: Int): Int
}