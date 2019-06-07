package com.zhufu.opencraft.util

import net.minecraft.server.v1_14_R1.ChatMessageType
import net.minecraft.server.v1_14_R1.IChatBaseComponent
import net.minecraft.server.v1_14_R1.PacketPlayOutChat
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer
import org.bukkit.entity.Player

object ActionBarTextUtil {
    fun sendActionText(player: Player,text: String){
        val type = ChatMessageType.GAME_INFO
        val packet = PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a("{\"text\":\"$text\"}"),type)
        (player as CraftPlayer).handle.playerConnection.sendPacket(packet)
    }
}