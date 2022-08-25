package com.zhufu.opencraft.listener

import com.zhufu.opencraft.getter
import com.zhufu.opencraft.special_item.SICompanion
import com.zhufu.opencraft.special_item.SpecialItem
import com.zhufu.opencraft.special_item.StatefulSpecialItem
import com.zhufu.opencraft.special_item.StatelessSpecialItem
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.plugin.Plugin
import kotlin.reflect.full.companionObjectInstance

object SpecialItemHandle : Listener {
    fun init(plugin: Plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)

        (StatefulSpecialItem.prebuilt + StatelessSpecialItem.prebuilt).forEach {
            val companion = (it.kotlin.companionObjectInstance as SICompanion?)
            companion?.recipe?.let { r ->
                Bukkit.addRecipe(r)
            }
        }
    }

    @EventHandler
    fun onCraft(event: CraftItemEvent) {
        val si = SpecialItem[event.recipe.result] ?: return
        if (si is StatefulSpecialItem) {
            val com = si::class.companionObjectInstance as SICompanion
            val player = Bukkit.getPlayer(event.whoClicked.uniqueId)!!
            event.inventory.result = com.newInstance(player.getter(), player)
        }
    }
}