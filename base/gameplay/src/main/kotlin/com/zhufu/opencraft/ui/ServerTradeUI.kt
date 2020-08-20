package com.zhufu.opencraft.ui

import com.zhufu.opencraft.DraggableInventory
import com.zhufu.opencraft.Info
import com.zhufu.opencraft.getter
import com.zhufu.opencraft.toInfoMessage
import org.bukkit.plugin.Plugin

class ServerTradeUI(owner: Info, plugin: Plugin) :
    DraggableInventory(plugin, 1, owner.getter()["rpg.ui.trade.title"].toInfoMessage()) {

}