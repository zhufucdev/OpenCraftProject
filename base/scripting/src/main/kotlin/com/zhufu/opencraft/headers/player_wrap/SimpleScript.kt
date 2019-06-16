package com.zhufu.opencraft.headers.player_wrap

import com.zhufu.opencraft.script.PlayerScript

@Suppress("unused")
class SimpleScript private constructor(private val wrap: PlayerScript) {
    val name get() = wrap.name
    val source get() = wrap.src
    val file get() = wrap.srcFile!!.path!!
    val author get() = wrap.player.name

    fun call() {
        wrap.reset()
        wrap.call()
    }

    companion object {
        fun from(script: PlayerScript) = SimpleScript(script)
        fun recover(simpleScript: SimpleScript) = simpleScript.wrap
    }
}