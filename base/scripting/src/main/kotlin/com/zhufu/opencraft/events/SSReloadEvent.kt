package com.zhufu.opencraft.events

import org.bukkit.event.HandlerList
import org.bukkit.event.server.ServerEvent

class SSReloadEvent: ServerEvent() {
    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {
        val handlers = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }
}