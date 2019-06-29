package com.zhufu.opencraft

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler

class HttpsDirector(private val hostName: String): HttpHandler {
    override fun handle(e: HttpExchange) {
        MajaroHandler.redirect("https://$hostName",e)
    }
}