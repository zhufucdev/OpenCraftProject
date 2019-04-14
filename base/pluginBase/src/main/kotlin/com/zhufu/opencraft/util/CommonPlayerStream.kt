package com.zhufu.opencraft.util

import com.google.gson.JsonElement
import com.zhufu.opencraft.PlayerStream
import net.minecraft.server.v1_13_R2.IChatBaseComponent
import net.minecraft.server.v1_13_R2.PacketPlayOutChat
import org.bukkit.ChatColor
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer
import org.bukkit.entity.Player
import java.io.File

class CommonPlayerStream(val player: Player) : PlayerStream() {
    companion object {
        fun standardChatMessage(sender: String,regularText: String,translatedText: String): String
                = "*<$sender> $translatedText ${ChatColor.GOLD}<->${ChatColor.RESET} $regularText"
    }

    override fun send(text: String) {
        player.sendMessage(text)
    }

    override fun sendRaw(json: JsonElement) {
        (player as CraftPlayer).handle.playerConnection.sendPacket(
            PacketPlayOutChat(
                IChatBaseComponent.ChatSerializer.a(json)
            )
        )
    }
    override fun sendChat(sender: String, regularText: String, translatedText: String, images: List<File>) = send(standardChatMessage(sender, regularText, translatedText))
}