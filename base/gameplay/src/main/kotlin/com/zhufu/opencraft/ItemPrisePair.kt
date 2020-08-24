package com.zhufu.opencraft

import org.bukkit.inventory.ItemStack

class ItemPrisePair(val item: ItemStack, prise: Prise<*>? = null) {
    val prise = prise ?: Prise.evaluate(item)
}