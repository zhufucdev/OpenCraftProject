package com.zhufu.opencraft.headers

import com.zhufu.opencraft.Header
import com.zhufu.opencraft.ServerPlayer
import com.zhufu.opencraft.headers.player_wrap.PlayerSelf

class PlayerHeader(private val player: ServerPlayer): Header {
    override val members: List<Pair<String, Any?>>
        get() = listOf(
            "self" to PlayerSelf.from(player)
        )
}