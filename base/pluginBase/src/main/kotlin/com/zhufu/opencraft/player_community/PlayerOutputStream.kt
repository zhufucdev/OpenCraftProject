package com.zhufu.opencraft.player_community

import com.google.gson.JsonElement
import com.zhufu.opencraft.ChatInfo
import com.zhufu.opencraft.Language
import java.io.File
import java.io.OutputStream

abstract class PlayerOutputStream : OutputStream() {
    protected var bf = ArrayList<Byte>()

    override fun write(b: Int) {
        bf.add(b.toByte())
    }

    override fun write(b: ByteArray) {
        b.forEach {
            bf.add(it)
        }
    }

    override fun flush() {
        send(bf.toByteArray().toString(Charsets.UTF_8))
        bf.clear()
    }

    abstract fun send(text: String)
    abstract fun sendChat(sender: ChatInfo, regularText: String, translatedText: String, images: List<File>)
    abstract fun sendChat(sender: ChatInfo, text: String)
    abstract fun sendRaw(json: JsonElement)

    abstract val lang: Language.LangGetter
    abstract val name: String
}