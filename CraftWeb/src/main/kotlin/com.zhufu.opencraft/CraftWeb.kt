package com.zhufu.opencraft

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import com.zhufu.opencraft.util.Language
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

class CraftWeb : JavaPlugin() {
    private lateinit var engine: ApplicationEngine
    private val port: Int
        get() = config.getInt("port")

    override fun onEnable() {
        instance = this
        try {
            init()
            engine.start()
            logger.info("Engine is running at port $port.")
        } catch (e: Exception) {
            logger.warning(Language.getDefault("web.error.whileInit", "HTTPS", e.javaClass.simpleName, e.message))
        }
    }

    override fun onDisable() {
        engine.stop()
    }

    private fun init() {
        engine = embeddedServer(Netty, port) {
            install(ContentNegotiation) {
                json()
            }
            routing {
                api()
            }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "web") {
            val getter = sender.getter()
            if (!sender.isOp) {
                sender.error(getter["command.error.permission"])
            }
            if (args.isEmpty()) {
                sender.apply {
                    info(getter["web.header"])
                    info(getter["web.${if (!engine.application.isEmpty) "running" else "stopped"}"])
                    info(getter["web.httpsPort", config.getInt("port")])
                }
            } else {
                when (args.first()) {
                    "start" -> {
                        val s =
                            if (!::engine.isInitialized) {
                                try {
                                    init()
                                    true
                                } catch (e: Exception) {
                                    sender.error(getter["web.error.whileInit", "HTTPS", e.javaClass.simpleName, e.message])
                                    e.printStackTrace()
                                    false
                                }
                            } else true

                        if (!s) {
                            sender.error(getter["command.error.failed"])
                            return true
                        }
                        try {
                            engine.start()
                            sender.success(getter["web.started"])
                        } catch (e: Exception) {
                            sender.error(getter["web.error.whileStarting", "HTTPS", e.javaClass.simpleName, e.message])
                            e.printStackTrace()
                            sender.error(getter["command.error.failed"])
                        }
                    }

                    "stop" -> {
                        try {
                            engine.stop(args.takeIf { it.size >= 2 }?.let { args[1].toLongOrNull() } ?: 0)
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
                            name.contains("port", true) -> {
                                val t = args[2].toIntOrNull()
                                if (t == null) {
                                    sender.error(getter["web.error.illegalArg", args[2], Int::class.simpleName])
                                    return true
                                }
                                t
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
                        commands.addAll(config.getKeys(false).filter { it.startsWith(args.last()) })
                        return commands
                    }
                }
            }
        }
        return mutableListOf()
    }

    companion object {
        lateinit var instance: CraftWeb
    }
}