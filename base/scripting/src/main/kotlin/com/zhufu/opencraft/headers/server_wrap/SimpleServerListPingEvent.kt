package com.zhufu.opencraft.headers.server_wrap

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import org.bukkit.Bukkit
import java.io.File

class SimpleServerListPingEvent constructor(private val wrap: PaperServerListPingEvent) {
    var motd
        get() = wrap.motd
        set(value) {
            wrap.motd = value
        }

    fun setIcon(file: File){
        wrap.serverIcon = Bukkit.getServer().loadServerIcon(file)
    }
    val playerSample get() = wrap.playerSample
    val client get() = wrap.client
}