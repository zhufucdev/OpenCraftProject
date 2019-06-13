package com.zhufu.opencraft

import com.google.gson.JsonElement
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.io.File
import java.io.OutputStream
import java.nio.charset.Charset

abstract class PlayerStream(private val maximumLength: Long = 1000) : OutputStream() {
    var bf = ArrayList<Byte>()
    private fun doCleaning() {
        if (bf.size > maximumLength){
            Bukkit.getLogger().warning("$name's stream is out of bound.")
            for (i in 1 .. bf.size - maximumLength){
                bf.removeAt(0)
            }
        }
    }

    override fun write(b: Int) {
        doCleaning()
        bf.add(b.toByte())
    }

    override fun write(b: ByteArray) {
        doCleaning()
        b.forEach {
            bf.add(it)
        }
    }

    override fun flush() {
        doCleaning()
        send(bf.toByteArray().toString(Charsets.UTF_8))
        bf.clear()
    }

    abstract fun send(text: String)
    abstract fun sendChat(sender: String, regularText: String, translatedText: String, images: List<File>)
    abstract fun sendRaw(json: JsonElement)

    abstract val lang: Language.LangGetter
    abstract val name: String
}