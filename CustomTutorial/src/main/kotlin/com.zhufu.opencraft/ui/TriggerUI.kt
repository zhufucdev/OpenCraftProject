package com.zhufu.opencraft.ui

import com.zhufu.opencraft.IntractableInventory
import com.zhufu.opencraft.Widgets
import com.zhufu.opencraft.info
import com.zhufu.opencraft.updateItemMeta
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toTipMessage
import org.bukkit.Bukkit
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class TriggerUI(plugin: Plugin) : IntractableInventory(plugin) {
    companion object {
        val helpDoc = listOf(
            "none -> 无触发方式",
            "block <x> <y> <z> -> 到达指定方块附近",
            "territory <名称> -> 进入指定领地",
            "enter-world <名称> -> 进入指定世界, 管理员操作",
            "register -> 注册服务器身份，管理员操作"
        )
    }

    override val inventory: Inventory = Bukkit.createInventory(null, 9 * helpDoc.size, "触发方式浏览器".toInfoMessage())

    init {
        helpDoc.forEachIndexed { index, s ->
            inventory.setItem(
                index * 9,
                Widgets.confirm
                    .updateItemMeta<ItemMeta> {
                        displayName("方式${index + 1}".toInfoMessage())
                        lore(listOf(s.toTipMessage()))
                    }
            )
        }
    }

    override fun onClick(event: InventoryClickEvent) {
        event.whoClicked.info("/ct trigger " + helpDoc[event.rawSlot / 9])
        close()
    }
}