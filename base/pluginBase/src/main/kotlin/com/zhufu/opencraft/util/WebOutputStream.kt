package com.zhufu.opencraft.util

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.zhufu.opencraft.ChatInfo
import com.zhufu.opencraft.Language
import com.zhufu.opencraft.WebInfo
import com.zhufu.opencraft.lang
import com.zhufu.opencraft.player_community.PlayerOutputStream
import java.io.File

class WebOutputStream(private val parent: WebInfo): PlayerOutputStream() {
    private val out
            get() = parent.lastExchange?.responseBody
    override fun send(text: String) {
        val content = text.toByteArray()
        parent.lastExchange?.apply {
            responseHeaders.add("Access-Control-Allow-Origin", "*")
            sendResponseHeaders(200, content.size.toLong())
        }
        out?.apply{
            write(content)
            close()
        }
    }

    override fun sendChat(sender: ChatInfo, regularText: String, translatedText: String, images: List<File>) {
        val json = JsonObject()
        json.apply {
            addProperty("r",0)
            addProperty("sender",sender.id)
            addProperty("raw",regularText)
            addProperty("translation",translatedText)
        }
        send("\$json:$json")
    }

    override fun sendRaw(json: JsonElement) {
        send(parent.lang()["translator.withJson",json.toString()])
    }

    override val lang: Language.LangGetter = parent.lang()
    override val name: String
        get() = "${parent.name ?: "unknown"} Web"
}