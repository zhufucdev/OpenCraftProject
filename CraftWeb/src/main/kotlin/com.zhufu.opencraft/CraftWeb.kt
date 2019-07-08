package com.zhufu.opencraft

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import HttpsServer
import SimpleExecutor
import org.bukkit.configuration.ConfigurationSection
import java.io.File
import java.util.logging.Logger

class CraftWeb : JavaPlugin() {
    private val instance = HttpsServer()
    private val http = HttpServer()

    companion object {
        const val unknown = "unknown"
        lateinit var logger: Logger
    }

    override fun onEnable() {
        Companion.logger = logger
        config.apply {
            if (!isSet("root")) {
                val default = File(dataFolder, "web").also { if (!it.exists()) it.mkdirs() }
                set("root", default.path)
            }
            if (!isSet("keyPath"))
                set("keyPath", File(dataFolder, "key.jks").path)
            if (!isSet("httpPort"))
                set("httpPort", 80)
            if (!isSet("httpsPort"))
                set("httpsPort", 443)
            if (!isSet("key"))
                set("key", "")
            if (!isSet("uiWhiteList"))
                set("uiWhiteList", listOf(""))
            if (!isSet("hostName"))
                set("hostName", "open-craft.cn")
            if (!isSet("chatPackLossThreshold"))
                set("chatPackLossThreshold", 10)
            if (!isSet("playerDirMaxSize"))
                set("playerDirMaxSize", 50 * 1024 * 1024)
            saveConfig()
        }
        ServerCaller.set<ConfigurationSection>("GetWebConfig") {
            return@set config
        }

        try {
            init()
            instance.start()
        } catch (e: Exception) {
            logger.warning(Language.getDefault("web.error.whileInit", "HTTPS", e.javaClass.simpleName, e.message))
        }
        try {
            initHttp()
            http.start()
        } catch (e: Exception) {
            logger.warning(Language.getDefault("web.error.whileInit", "HTTP", e.javaClass.simpleName, e.message))
        }
    }

    override fun onDisable() {
        instance.stop(0)
        http.stop(0)
    }

    private fun init() {
        instance.init(
            key = File(config.getString("keyPath")!!),
            password = config.getString("key")!!,
            port = config.getInt("httpsPort"),
            handler = MajaroHandler(File(config.getString("root")!!), config),
            executor = SimpleExecutor()
        )
    }

    private fun initHttp() {
        http.init(
            port = config.getInt("httpPort"),
            handler = HttpsDirector(config.getString("hostName")!!)
        )
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "web") {
            val getter = sender.lang()
            if (!sender.isOp) {
                sender.error(getter["command.error.permission"])
            }
            if (args.isEmpty()) {
                sender.apply {
                    info(getter["web.header"])
                    info(getter["web.${if (instance.isRunning) "running" else "stopped"}"])
                    info(getter["web.rootPath", config.getString("root", unknown)])
                    info(getter["web.keyPath", config.getString("keyPath", unknown)])
                    info(getter["web.key", config.getString("key", unknown)])
                    info(getter["web.httpPort", config.getInt("httpPort")])
                    info(getter["web.httpsPort", config.getInt("httpsPort")])
                }
            } else {
                when (args.first()) {
                    "start" -> {
                        val s =
                            if (!instance.isInitialized || !http.isInitialized) {
                                try {
                                    init()
                                    true
                                } catch (e: Exception) {
                                    sender.error(getter["web.error.whileInit", "HTTPS", e.javaClass.simpleName, e.message])
                                    e.printStackTrace()
                                    false
                                } && try {
                                    initHttp()
                                    http.start()
                                    true
                                } catch (e: Exception) {
                                    sender.error(
                                        Language.getDefault(
                                            "web.error.whileInit",
                                            "HTTP",
                                            e.javaClass.simpleName,
                                            e.message
                                        )
                                    )
                                    e.printStackTrace()
                                    false
                                }
                            } else true

                        if (!s) {
                            sender.error(getter["command.error.failed"])
                            return true
                        }
                        try {
                            instance.start()
                            sender.success(getter["web.started"])
                        } catch (e: Exception) {
                            sender.error(getter["web.error.whileStarting", "HTTPS", e.javaClass.simpleName, e.message])
                            e.printStackTrace()
                            sender.error(getter["command.error.failed"])
                        }
                    }
                    "stop" -> {
                        try {
                            instance.stop(if (args.size >= 2) args[1].toIntOrNull() ?: 0 else 0)
                            sender.success(getter["web.stopped"])
                        } catch (e: Exception) {
                            sender.error(getter["web.error.whileStopping", "HTTPS", e::class.simpleName, e.message])
                            sender.error(getter["command.error.failed"])
                        }
                    }
                    "reload" -> {
                        try {
                            reloadConfig()
                            init()
                        } catch (e: Exception) {
                            sender.error(getter["web.error.whileInit", "HTTPS", e.javaClass.simpleName, e.message])
                            e.printStackTrace()
                        }
                        try {
                            initHttp()
                            http.start()
                        } catch (e: Exception) {
                            sender.error(
                                Language.getDefault(
                                    "web.error.whileInit",
                                    "HTTP",
                                    e.javaClass.simpleName,
                                    e.message
                                )
                            )
                            e.printStackTrace()
                        }
                    }
                    "set" -> {
                        if (args.size < 3) {
                            sender.error(getter["command.error.tooFewArgs", 3])
                            return false
                        }
                        val name = args[1]
                        if (!config.isSet(name)) {
                            sender.error(getter["web.error.unknownArg", name])
                            return true
                        }
                        val value: Any = when {
                            name.contains("port", true)
                                    || name == "chatPackLossThreshold"
                                    || name == "playerDirMaxSize" -> {
                                val t = args[2].toIntOrNull()
                                if (t == null) {
                                    sender.error(getter["web.error.illegalArg", args[2], Int::class.simpleName])
                                    return true
                                }
                                t
                            }
                            name == "uiWhiteList" -> {
                                val t = ArrayList<String>()
                                for (i in 2 until args.size) {
                                    t.add(args[i])
                                }
                                t.toList()
                            }
                            else -> args[2]
                        }
                        config.set(name, value)
                        saveConfig()
                        sender.success(getter["web.set", name, value])
                    }
                }
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (command.name == "web") {
            if (sender.isOp) {
                when {
                    args.size <= 1 -> {
                        val commands = mutableListOf("start", "stop", "set", "reload")
                        if (args.isNotEmpty()) {
                            commands.removeAll { !it.startsWith(args.first()) }
                        }
                        return commands
                    }
                    args.first() == "set" && args.size < 3 -> {
                        val commands = ArrayList<String>()
                        commands.addAll(config.getKeys(false))
                        if (args.size == 2) {
                            commands.removeAll { !it.startsWith(args.last()) }
                        }
                        return commands
                    }
                }
            }
        }
        return mutableListOf()
    }
}