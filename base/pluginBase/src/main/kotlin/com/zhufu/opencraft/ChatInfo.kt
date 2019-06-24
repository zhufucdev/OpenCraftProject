package com.zhufu.opencraft

interface ChatInfo {
    var doNotTranslate: Boolean
    val displayName: String
    val targetLang: String
    val id: String

    val playerStream: PlayerStream
}