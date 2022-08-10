package com.zhufu.opencraft.util

import com.google.gson.JsonElement
import com.zhufu.opencraft.ChatInfo
import com.zhufu.opencraft.Language
import com.zhufu.opencraft.getter
import com.zhufu.opencraft.player_community.PlayerOutputStream
import net.kyori.adventure.text.Component
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.io.File

class CommonPlayerOutputStream(val player: Player) : PlayerOutputStream() {
    override fun send(text: String) {
        player.sendMessage(text)
    }

    override fun send(component: Component) {
        player.sendMessage(component)
    }

    override fun sendRaw(json: JsonElement) {
        TODO()
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