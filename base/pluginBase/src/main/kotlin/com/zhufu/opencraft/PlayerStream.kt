package com.zhufu.opencraft

import com.google.gson.JsonElement
import java.io.File

abstract class PlayerStream {
    abstract fun send(text: String)
    abstract fun sendChat(sender: String,regularText: String,translatedText: String, images: List<File>)
    abstract fun sendRaw(json: JsonElement)
}