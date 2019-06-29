package com.zhufu.opencraft.util

import com.google.gson.JsonElement
import com.zhufu.opencraft.ChatInfo
import com.zhufu.opencraft.Language
import com.zhufu.opencraft.player_community.PlayerOutputStream
import com.zhufu.opencraft.lang
import net.minecraft.server.v1_14_R1.IChatBaseComponent
import net.minecraft.server.v1_14_R1.PacketPlayOutChat
import org.bukkit.ChatColor
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import java.io.File

class CommonPlayerOutputStream(val player: Player) : PlayerOutputStream() {
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

    override fun sendChat(sender: ChatInfo, regularText: String, translatedText: String, images: List<File>) =
        send("*<${sender.displayName}> $translatedText ${ChatColor.GOLD}<->${ChatColor.RESET} $regularText")

    override val lang: Language.LangGetter
        get() = player.lang()
    override val name: String
        get() = player.name
}