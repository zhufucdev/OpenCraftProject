package com.zhufu.opencraft

import org.bukkit.entity.Player

interface Backable {
    val parentInventory: IntractableInventory?
    fun back(showTo: Player) = parentInventory?.show(showTo) ?: showTo.closeInventory()
}