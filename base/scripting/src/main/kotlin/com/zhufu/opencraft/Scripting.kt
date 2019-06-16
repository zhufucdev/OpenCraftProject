package com.zhufu.opencraft

import com.zhufu.opencraft.headers.player_wrap.PlayerSelf
import com.zhufu.opencraft.script.AbstractScript
import java.util.*
import kotlin.collections.HashMap

object Scripting {
    // Data
    val version get() = "beta2"
    var timeOut = 5000L
    var id = 0

    fun cleanUp() {
        PlayerSelf.cache.cleanUp()
        loopExecutions.clear()
        AbstractScript.threadPool.shutdown()
    }

    // Run
    enum class Executor {
        Sever, Player, Operator
    }
    val loopExecutions = HashMap<UUID, Long>()
}