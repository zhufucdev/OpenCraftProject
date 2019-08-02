package com.zhufu.opencraft

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import java.util.logging.Logger

abstract class MyHandler(private val logger: Logger? = null): HttpHandler {
    abstract fun handleWithExceptions(exchange: HttpExchange)
    final override fun handle(exchange: HttpExchange) {
        try {
            handleWithExceptions(exchange)
        } catch (e: Exception){
            logger?.warning("An exception caught while handling the connection for ${exchange.remoteAddress.hostName}.")
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