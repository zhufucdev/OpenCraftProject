package com.zhufu.opencraft

import com.google.gson.JsonElement
import java.io.File
import java.net.InetAddress
import java.nio.file.Paths
import java.util.*

class RegisteredInfo(uuid: UUID) : WebInfo(false, uuid), ChatInfo {
    override val tagFile: File
        get() = Paths.get("plugins", "tag", "$uuid.yml").toFile()

    override val id: String
        get() = name?:"unknown"
    override var doNotTranslate = false
    override val displayName get() = name!!
    override val targetLang: String get() = this.userLanguage
    override val playerStream: PlayerStream = object : PlayerStream() {
        override fun sendRaw(json: JsonElement) {}
        override fun send(text: String) {}
        override fun sendChat(sender: String, regularText: String, translatedText: String, images: List<File>) {}
    }
    override val face: File
        get() = File("plugins/faces/$uuid.png")
}