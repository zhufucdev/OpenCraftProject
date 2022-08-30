package com.zhufu.opencraft

import org.bukkit.entity.HumanEntity

interface Backable {
    val parentInventory: IntractableInventory?
    fun back(showTo: HumanEntity) = parentInventory?.show(showTo) ?: showTo.closeInventory()
}