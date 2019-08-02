package com.zhufu.opencraft

import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsParameters
import com.sun.net.httpserver.HttpsServer
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.net.InetSocketAddress
import java.security.KeyStore
import java.util.concurrent.Executor
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class HttpsServer {
    private lateinit var server: HttpsServer
    var isRunning = false
        private set
    var isInitialized = false
        private set
    private var port = 0
    fun init(key: File, password: String, port: Int, handler: HttpHandler? = null, executor: Executor? = null) {
        if (isInitialized) {
            server.stop(0)
            isInitialized = false
        }
        if (!key.exists()) {
            throw FileNotFoundException("Key file at ${key.path} must exists!")
        }

        server = HttpsServer.create(InetSocketAddress(port), 0)
        this.port = port
        isInitialized = true

        val ssl = SSLContext.getInstance("TLS")
        val ks = KeyStore.getInstance("JKS")
        val fis = FileInputStream(key)
        val pwd = password.toCharArray()
        ks.load(fis, pwd)

        val kmf = KeyManagerFactory.getInstance("SunX509")
        kmf.init(ks, pwd)

        val tmf = TrustManagerFactory.getInstance("SunX509")
        tmf.init(ks)

        ssl.init(kmf.keyManagers, tmf.trustManagers, null)
        server.httpsConfigurator = object : HttpsConfigurator(ssl) {
            override fun configure(p: HttpsParameters) {
                super.configure(p)
                val context = SSLContext.getDefault()
                val engine = context.createSSLEngine()

                p.apply {
                    needClientAuth = false
                    cipherSuites = engine.enabledCipherSuites
                    protocols = engine.enabledProtocols

                    setSSLParameters(context.defaultSSLParameters)
                }
            }
        }
        if (handler != null)
            server.createContext("/", handler)
        server.executor = executor
    }

    private fun check() {
        if (!isInitialized)
            throw IllegalStateException("Server hasn't been initialized yet! Call [init] first.")
    }

    fun start() {
        check()
        server.start()
        isRunning = true
    }

    fun stop(i: Int) {
        check()
        server.stop(i)
        server = HttpsServer.create(InetSocketAddress(port), 0)
        isRunning = false
    }
}