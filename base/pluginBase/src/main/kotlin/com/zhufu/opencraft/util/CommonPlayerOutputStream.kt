package com.zhufu.opencraft.util

import com.google.gson.JsonElement
import com.zhufu.opencraft.ChatInfo
import com.zhufu.opencraft.Language
import com.zhufu.opencraft.player_community.PlayerOutputStream
import com.zhufu.opencraft.getter
import net.minecraft.server.v1_16_R1.ChatMessageType
import net.minecraft.server.v1_16_R1.IChatBaseComponent
import net.minecraft.server.v1_16_R1.PacketPlayOutChat
import org.bukkit.ChatColor
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import java.io.File
import java.util.*

class CommonPlayerOutputStream(val player: Player) : PlayerOutputStream() {
    override fun send(text: String) {
        player.sendMessage(text)
    }

    override fun sendRaw(json: JsonElement) {
        (player as CraftPlayer).handle.playerConnection.sendPacket(
            PacketPlayOutChat(
                IChatBaseComponent.ChatSerializer.a(json),
                ChatMessageType.SYSTEM,
                UUID.randomUUID()
            )
        )
    }

    override fun sendChat(sender: ChatInfo, regularText: String, translatedText: String, images: List<File>) =
        send("*<${sender.displayName}> $translatedText ${ChatColor.GOLD}<->${ChatColor.RESET} $regularText")

    override fun sendChat(sender: ChatInfo, text: String) {
        send("<${sender.displayName}> $text")
    }

    override val lang: Language.LangGetter
        get() = player.getter()
    override val name: String
        get() = player.name
}