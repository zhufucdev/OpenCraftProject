package com.zhufu.opencraft.headers

import com.zhufu.opencraft.Header
import com.zhufu.opencraft.Scripting
import com.zhufu.opencraft.ServerPlayer
import com.zhufu.opencraft.headers.player_wrap.PlayerSelf

class PlayerHeaders(private val player: ServerPlayer, val executor: Scripting.Executor): Header {
    override val members: List<Pair<String, Any?>>
        get() = listOf(
            "self" to PlayerSelf.from(player,executor)
        )
}