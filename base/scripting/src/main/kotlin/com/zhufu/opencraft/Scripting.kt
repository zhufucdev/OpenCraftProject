package com.zhufu.opencraft

import com.zhufu.opencraft.headers.player_wrap.PlayerSelf

object Scripting {
    // Data
    val version get() = "beta2"
    var timeOut = 5000L
    var id = 0

    fun cleanUp() {
        PlayerSelf.cache.cleanUp()
        loopExecutions.clear()
    }

    // Run
    enum class Executor {
        Sever, Player, Operator
    }
    val loopExecutions = HashMap<Int, Long>()
}