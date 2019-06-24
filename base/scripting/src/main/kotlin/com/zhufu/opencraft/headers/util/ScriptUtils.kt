package com.zhufu.opencraft.headers.util

import com.zhufu.opencraft.ServerPlayer
import com.zhufu.opencraft.getLang
import com.zhufu.opencraft.headers.player_wrap.SimpleScript
import com.zhufu.opencraft.script.PlayerScript

@Suppress("unused")
class ScriptUtils(private val serverInfo: ServerPlayer) {
    fun get(name: String) = SimpleScript.from(PlayerScript.list(serverInfo).firstOrNull { it.name == name }
        ?: throw IllegalArgumentException(getLang(serverInfo,"scripting.scriptNotFound",name)))

    fun call(name: String) = get(name).call()
}