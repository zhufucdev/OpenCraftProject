package com.zhufu.opencraft

object Translator {
    lateinit var handler: (String,ChatInfo) -> Boolean

    var lastChat: Long = 0
        private set
    fun chat(msg: String, info: ChatInfo) {
        lastChat = System.currentTimeMillis()
        handler.invoke(msg,info)
    }
}