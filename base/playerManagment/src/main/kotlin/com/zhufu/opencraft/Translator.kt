package com.zhufu.opencraft

object Translator {
    lateinit var handler: (String,ChatInfo) -> Boolean

    fun chat(msg: String, info: ChatInfo) = handler.invoke(msg,info)
}