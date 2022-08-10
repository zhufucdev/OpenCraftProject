package com.zhufu.opencraft.util

import org.bukkit.entity.Player

object ActionBarTextUtil {
    @Deprecated("Not safe", ReplaceWith("Player.sendActionBar"))
    fun sendActionText(player: Player, text: String) {
        throw NotImplementedError()
    }
}