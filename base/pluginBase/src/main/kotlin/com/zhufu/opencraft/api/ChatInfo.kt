package com.zhufu.opencraft.api

import com.zhufu.opencraft.player_community.PlayerOutputStream

interface ChatInfo {
    var doNotTranslate: Boolean
    val displayName: String
    val targetLang: String
    val id: String

    val playerOutputStream: PlayerOutputStream
}