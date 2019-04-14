package com.zhufu.opencraft.listener

import net.citizensnpcs.api.npc.NPC
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class NPCSelectListener(array: Array<NPC>) : Listener {
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent){

    }
}