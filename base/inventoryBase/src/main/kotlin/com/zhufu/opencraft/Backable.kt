package com.zhufu.opencraft

import org.bukkit.entity.Player

interface Backable {
    val parentInventory: ClickableInventory
    fun back(showTo: Player) = parentInventory.show(showTo)
}