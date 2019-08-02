package com.zhufu.opencraft.listener

import com.zhufu.opencraft.Core
import com.zhufu.opencraft.NPCExtend
import com.zhufu.opencraft.PlayerManager
import net.citizensnpcs.api.event.NPCRightClickEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

object NPCListener : Listener {
    @EventHandler
    fun onClick(event: NPCRightClickEvent){
        val click = event.npc.data().get<String>("click")?: return
        if (click != "none") {
            NPCExtend.doNPCText(click,event.clicker)
        }
    }
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent){
        Core.npc.forEach {
            if (!it.data().has("near"))
                return
            val info = PlayerManager.findInfoByPlayer(event.player)?:return
            val near = if ((it.storedLocation?:return).world == event.player.world) it.storedLocation.distance(event.player.location) <= 3 else false
            val said = info.tag.getBoolean("${it.name}Said",false)
            if (!said && near){
                NPCExtend.doNPCText(it.data()["near"],event.player)
                info.tag.set("${it.name}Said",true)
            }
            else if (said && !near){
                info.tag.set("${it.name}Said",false)
            }
        }
    }
}