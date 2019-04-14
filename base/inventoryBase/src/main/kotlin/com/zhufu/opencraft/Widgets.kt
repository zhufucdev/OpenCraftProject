package com.zhufu.opencraft

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object Widgets {
    val confirm
        get() = ItemStack(Material.LIME_DYE)
    val cancel
        get() = ItemStack(Material.ROSE_RED)
    val group
        get() = ItemStack(Material.CHEST)
    val back
        get() = ItemStack(Material.ARROW)
    val rename
        get() = ItemStack(Material.SIGN)
    val close
        get() = ItemStack(Material.BARRIER)
}