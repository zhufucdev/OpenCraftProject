package com.zhufu.opencraft

import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executor

class HttpServer {
    private lateinit var server: HttpServer
    var isRunning = false
        private set
    var isInitialized = false
        private set
    private var port = 0
    fun init(port: Int, handler: HttpHandler? = null, executor: Executor? = null) {
        if (isInitialized) {
            server.stop(0)
            isInitialized = false
        }

        server = HttpServer.create(InetSocketAddress(port), 0)
        this.port = port
        isInitialized = true
        if (handler != null)
            server.createContext("/", handler)
        server.executor = executor
    }

    private fun check() {
        if (!isInitialized)
            throw IllegalStateException("Server hasn't been initialized yet! Call #init first.")
    }

    fun start() {
        check()
        server.start()
        isRunning = true
    }

    fun stop(i: Int) {
        check()
        server.stop(i)
        server = HttpServer.create(InetSocketAddress(port), 0)
        isRunning = false
    }
}