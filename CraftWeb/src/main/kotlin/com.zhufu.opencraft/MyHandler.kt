package com.zhufu.opencraft

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.zhufu.opencraft.CraftWeb.Companion.logger
import java.io.PrintStream
import java.io.StringWriter

abstract class MyHandler: HttpHandler {
    abstract fun handleWithExceptions(exchange: HttpExchange)
    final override fun handle(exchange: HttpExchange) {
        try {
            handleWithExceptions(exchange)
        } catch (e: Exception){
            logger.warning("An exception caught while handling the connection for ${exchange.remoteAddress.hostName}.")
            e.printStackTrace()
            val content = "${e::class.simpleName}: ${e.message}".toByteArray()
            with(exchange) {
                responseHeaders.add("Access-Control-Allow-Origin", "*")
                sendResponseHeaders(-1, content.size.toLong())
                responseBody.apply {
                    write(content)
                    close()
                }
            }
        }
    }
}