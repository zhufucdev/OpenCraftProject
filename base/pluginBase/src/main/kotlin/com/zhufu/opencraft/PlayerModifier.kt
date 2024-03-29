package com.zhufu.opencraft

import com.zhufu.opencraft.data.Info
import org.bukkit.GameMode

class PlayerModifier(val info: Info) {
    val player by lazy { info.player }

    var isFlyable = player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR

    fun apply() {
        info.player.allowFlight = isFlyable
    }
}