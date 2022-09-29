package com.zhufu.opencraft.util

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.zhufu.opencraft.api.ChatInfo
import com.zhufu.opencraft.data.WebInfo
import com.zhufu.opencraft.getter
import com.zhufu.opencraft.player_community.PlayerOutputStream
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import java.io.File

class WebOutputStream(private val parent: WebInfo): PlayerOutputStream() {
    override fun send(text: String) {
        TODO("Implementation")
    }

    override fun sendChat(sender: ChatInfo, regularText: String, translatedText: String, images: List<File>) {
        val json = JsonObject()
        json.apply {
            addProperty("r",0)
            addProperty("sender",sender.displayName)
            addProperty("raw",regularText)
            addProperty("translation",translatedText)
        }
        send("\$json:$json")
    }

    override fun sendChat(sender: ChatInfo, text: String) {
        send("${sender.displayName}: $text")
    }

    override fun send(component: Component) {
        if (component is TextComponent) {
            send(component.content())
        } else {
            throw NotImplementedError()
        }
    }

    @Deprecated("Not safe", replaceWith = ReplaceWith("send(Component)"))
    override fun sendRaw(json: JsonElement) {
        send(parent.getter()["translator.withJson",json.toString()])
    }

    override val lang: Language.LangGetter = parent.getter()
    override val name: String
        get() = "${parent.name ?: "unknown"} Web"
}