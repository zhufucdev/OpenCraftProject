package com.zhufu.opencraft.api

import com.zhufu.opencraft.player_community.PlayerOutputStream
import java.util.UUID

interface ChatInfo {
    var doNotTranslate: Boolean
    val displayName: String
    val targetLang: String
    val uuid: UUID

    val playerOutputStream: PlayerOutputStream
}