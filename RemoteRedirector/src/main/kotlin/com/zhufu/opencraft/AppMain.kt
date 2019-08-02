package com.zhufu.opencraft

import com.google.gson.JsonParser
import java.io.File
import kotlin.system.exitProcess

private fun init(config: File) {
    fun checkArgError(name: String) {
        throw IllegalArgumentException("Config file doesn't have an element [$name]!")
    }

    println("Using config file ${config.absolutePath}.")
    if (config.exists()) {
        val json = JsonParser().parse(config.bufferedReader()).asJsonObject
        if (!json.has("address"))
            checkArgError("address")
        if (!json.has("key"))
            checkArgError("key")
        if (!json.has("password"))
            checkArgError("password")
        init(
            json["address"].asString,
            json["isHttps"]?.asBoolean ?: false,
            File(json["key"].asString),
            json["password"].asString,
            json["httpPort"]?.asInt ?: 80,
            json["httpsPort"]?.asInt ?: 443
        )
    } else {
        println("Configuration file ${config.path} doesn't exist!")
        printUsageAndExit()
    }
}

private val httpServer = HttpServer()
private val httpsServer = HttpsServer()
private lateinit var handler: HttpHandler

private fun init(
    host: String,
    isHttps: Boolean,
    keyFile: File,
    password: String,
    localHttpPort: Int = 80,
    localHttpsPort: Int = 443
) {
    handler = HttpHandler(
        remoteHost = host,
        isHttps = isHttps
    )
    httpServer.init(
        port = localHttpPort,
        handler = handler,
        executor = SimpleExecutor()
    )
    httpServer.start()
    httpsServer.init(
        key = keyFile,
        password = password,
        port = localHttpsPort,
        handler = handler,
        executor = SimpleExecutor()
    )
    httpsServer.start()
}

private fun printUsageAndExit() {
    println(
        "usage: -host=<Host Name> [-https] -key=<Path to Https Key File> " +
                "-password=<Password to Key File>"
    )
    println("       -config=<Path to Configuration File>")
    exitProcess(1)
}

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        val fileArg = args.firstOrNull { it.startsWith("-config=") }?.substring(8)
        if (fileArg != null) {
            init(File(fileArg))
        } else {
            val host = args.firstOrNull { it.startsWith("-host=") }?.substring(6)
            if (host == null) {
                printUsageAndExit()
            } else {
                val https = args.contains("https")
                val keyPath = args.firstOrNull { it.startsWith("-key=") }?.substring(5)
                if (keyPath == null) {
                    println("Expecting an argument key, but getting nothing.")
                    printUsageAndExit()
                }
                val password = args.firstOrNull { it.startsWith("-password=") }?.substring(10)
                if (password == null) {
                    println("Expecting an argument password, but getting nothing.")
                    printUsageAndExit()
                }
                init(host, https, File(keyPath!!), password!!)
            }
        }
    } else {
        init(File("config.json"))
    }
    while (true) {
        val instruction = readLine()
        if (instruction == "stop") {
            httpsServer.stop(0)
            httpServer.stop(0)
            break
        }
    }
}