package com.zhufu.opencraft.headers

import com.zhufu.opencraft.Header
import com.zhufu.opencraft.Scripting
import com.zhufu.opencraft.ServerPlayer
import com.zhufu.opencraft.headers.player_wrap.PlayerSelf
import com.zhufu.opencraft.headers.util.ScriptUtils

class PlayerHeaders(private val player: ServerPlayer, private val executor: Scripting.Executor): Header {
    private val su = ScriptUtils(player)
    override val members: List<Pair<String, Any?>>
        get() = listOf(
            "self" to PlayerSelf.from(player,executor),
            "script" to su
        )
}