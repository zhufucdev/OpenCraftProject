package com.zhufu.opencraft

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.zhufu.opencraft.CraftWeb.Companion.logger

abstract class MyHandler: HttpHandler {
    abstract fun handleWithExceptions(exchange: HttpExchange)
    final override fun handle(exchange: HttpExchange) {
        try {
            handleWithExceptions(exchange)
        } catch (e: Exception){
            logger.warning("An exception caught while handling the connection for ${exchange.remoteAddress.hostName}.")
            e.printStackTrace()
        }
    }
}