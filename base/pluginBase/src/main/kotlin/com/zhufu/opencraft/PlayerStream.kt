package com.zhufu.opencraft

import com.google.gson.JsonElement
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.io.File
import java.io.OutputStream
import java.nio.charset.Charset

abstract class PlayerStream : OutputStream() {
    var bf = ArrayList<Byte>()

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
    abstract fun sendChat(sender: String, regularText: String, translatedText: String, images: List<File>)
    abstract fun sendRaw(json: JsonElement)

    abstract val lang: Language.LangGetter
    abstract val name: String
}