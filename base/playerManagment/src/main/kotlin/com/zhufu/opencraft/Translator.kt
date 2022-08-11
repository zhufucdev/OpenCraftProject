package com.zhufu.opencraft

import com.zhufu.opencraft.api.ChatInfo

object Translator {
    lateinit var handler: (String, ChatInfo) -> Boolean

    fun chat(msg: String, info: ChatInfo) {
        handler.invoke(msg, info)
    }
}