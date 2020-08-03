package com.zhufu.opencraft

import com.zhufu.opencraft.Base.Extend.isDigit
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.survey.SurveyManager
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToLong
import kotlin.reflect.full.functions
import kotlin.system.measureTimeMillis

class ServerCommandExecutor(private val plugin: Core) : TabExecutor {
    private val dataFolder get() = plugin.dataFolder
    private val server get() = plugin.server

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        when {
            args.isEmpty() -> {
                sender.sendMessage("用法错误")
                return false
            }
            args.first() == "feedback" -> {
                if (args.size < 2) {
                    sender.sendMessage(TextUtil.error("您没有输入文本"))
                    return false
                }
                val sb = StringBuilder()
                for (i in 1 until args.size)
                    sb.append("${args[i]} ")
                sb.deleteCharAt(sb.length - 1)

                if (sb.length > 300) {
                    sender.sendMessage(TextUtil.error("字数超过上限(300字)"))
                    return true
                }

                val file = File(
                    "$dataFolder${File.separatorChar}feedback${File.separatorChar}${SimpleDateFormat("yyyy${File.separatorChar}MM${File.separatorChar}dd${File.separatorChar}HH:mm").format(
                        Date()
                    )}-${sender.name}.txt"
                )
                file.parentFile.mkdirs()
                file.createNewFile()
                file.writeText(sb.toString())

                PlayerManager.forEachOffline {
                    if (it.offlinePlayer.isOp) {
                        it.messagePool.add(
                            TextUtil.info("收到来自${sender.name} 的反馈:") + sb.toString(),
                            MessagePool.Type.Friend
                        )
                    }
                }

                sender.sendMessage(TextUtil.info("您的反馈已提交，${TextUtil.success("感谢支持")}"))
            }
            args.first() == "about" -> {
                val readme = File(dataFolder, "readme.txt")
                if (!readme.exists()) {
                    sender.sendMessage(TextUtil.info("未找到自述文件"))
                    return true
                }
                val charset = if (args.size >= 2) try {
                    Charset.forName(args[1])
                } catch (e: Exception) {
                    sender.sendMessage(TextUtil.error("未找到编码格式: ${args[1]}"))
                    return false
                } else Charsets.UTF_8
                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    val path = Paths.get(
                        System.getenv("PATH").split(':').firstOrNull {
                            Paths.get(it, "screenfetch").toFile().exists()
                        } ?: "null",
                        "screenfetch"
                    ).toFile()
                    val screenfetch = if (path.exists()) {
                        Runtime.getRuntime().exec(path.absolutePath)
                            .inputStream.bufferedReader()
                            .readText().filter { !it.isISOControl() || it == '\n' }
                            .replace(Regex("\\[[0-9]*;"), "[")
                            .replace("[0m", "")
                            .replace("[1m", ChatColor.RESET.toString())
                            .replace("[30m", "${ChatColor.COLOR_CHAR}${ChatColor.BLACK.char}")
                            .replace("[31m", "${ChatColor.COLOR_CHAR}${ChatColor.RED.char}")
                            .replace("[32m", "${ChatColor.COLOR_CHAR}${ChatColor.GREEN.char}")
                            .replace("[33m", "${ChatColor.COLOR_CHAR}${ChatColor.YELLOW.char}")
                            .replace("[34m", "${ChatColor.COLOR_CHAR}${ChatColor.BLUE.char}")
                            .replace("[35m", "${ChatColor.COLOR_CHAR}${ChatColor.LIGHT_PURPLE.char}")
                            .replace("[36m", "${ChatColor.COLOR_CHAR}${ChatColor.DARK_AQUA.char}")
                            .replace("[37m", "${ChatColor.COLOR_CHAR}${ChatColor.WHITE}")
                            .plus(ChatColor.RESET.char)
                    } else "ERROR: no shell command screenfetch.\n"
                    sender.sendMessage(
                        TextUtil.format(
                            title = "自述文件",
                            content = screenfetch + readme.readText(charset)
                        )
                    )
                })
            }
            args.first() == "stop" -> {
                if (sender !is ConsoleCommandSender && !sender.isOp) {
                    sender.sendMessage(TextUtil.error("您没有权限使用此命令"))
                    return true
                }
                if (args.size < 2) {
                    sender.sendMessage(TextUtil.error("请给出原因"))
                    return false
                }
                val message = TextUtil.info(args[1])
                server.onlinePlayers.forEach {
                    it.sendTitle(TextUtil.error("服务器即将关闭"), message, 7, 1000, 7)
                }
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    Bukkit.getOnlinePlayers().forEach {
                        it.kickPlayer(message)
                    }
                    server.shutdown()
                }, (args[1].length * 0.2 * 20).roundToLong())
            }
            args.first() == "reload" -> {
                if (sender !is ConsoleCommandSender && !sender.isOp) {
                    sender.sendMessage(TextUtil.error("您没有权限使用此命令"))
                    return true
                }
                if (args.size < 2) {
                    Bukkit.getPluginManager().callEvent(ServerReloadEvent())
                    sender.info("正在重载服务器")
                    return true
                }

                val failure = ArrayList<String>()
                (1 until args.size).forEach {
                    var result = false
                    when (args[it]) {
                        "game" -> {
                            sender.sendMessage(TextUtil.info("正在重载游戏"))
                            result = true
                            GameManager.forEach { game ->
                                val id = game.gameID
                                try {
                                    game.initGameEnder()
                                } catch (e: Exception) {
                                    result = false
                                    sender.sendMessage(TextUtil.error("gameID: $id ${e.javaClass.simpleName}: ${e.message}"))
                                    e.printStackTrace()
                                }
                            }
                        }
                        "npc" -> {
                            sender.sendMessage(TextUtil.info("正在重载NPC"))
                            result = try {
                                plugin.spawnNPC()
                                true
                            } catch (e: Exception) {
                                sender.sendMessage(TextUtil.error("${e.javaClass.simpleName}: ${e.message}"))
                                e.printStackTrace()
                                false
                            }
                        }
                        "survey" -> {
                            sender.sendMessage(TextUtil.info("正在重载服务器调查"))
                            SurveyManager.init(File(dataFolder, "survey.json"), null)
                            result = true
                        }
                        "lang" -> {
                            sender.info("正在重载语言文件")
                            result = try {
                                Language.init()
                                true
                            } catch (e: Exception) {
                                sender.sendMessage(TextUtil.error("${e.javaClass.simpleName}: ${e.message}"))
                                e.printStackTrace()
                                false
                            }
                        }
                        "ss" -> {
                            sender.info("正在重载脚本")
                            try {
                                val plugin = Bukkit.getPluginManager().getPlugin("ServerScript")
                                if (plugin != null) {
                                    plugin::class.functions.first { kFunction -> kFunction.name == "initScripting" }
                                        .call(plugin)
                                    result = true
                                } else {
                                    Bukkit.getLogger().warning("ServerScript plugin isn't loaded")
                                }
                            } catch (e: Exception) {
                                sender.error("${e.javaClass.simpleName}: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                        "builder" -> {
                            sender.info("正在重载建筑者配置")
                            result = try {
                                plugin.reloadConfig()
                                true
                            } catch (e: Exception) {
                                sender.error("${e.javaClass.simpleName}: ${e.message}")
                                e.printStackTrace()
                                false
                            }
                        }
                        else -> {
                            sender.sendMessage(TextUtil.error("无效模块: ${args[1]}"))
                        }
                    }
                    if (!result)
                        failure.add(args[it])
                }
                sender.sendMessage(
                    if (failure.isEmpty())
                        TextUtil.info("成功重载指定模块")
                    else {
                        buildString {
                            append("${ChatColor.RED}无法重载")
                            failure.forEach { append("$it,") }
                            deleteCharAt(lastIndex)
                        }
                    }
                )
            }
            args.first() == "set" -> {
                if (!sender.isOp) {
                    sender.sendMessage(TextUtil.error("您没有权限使用此命令"))
                    return true
                }
                if (args.size < 3) {
                    sender.sendMessage(TextUtil.error("用法错误"))
                    return true
                }
                when (args[1]) {
                    "url" -> {
                        Game.env.set("url", args[2])
                        Game.env.save(File(dataFolder, "env"))
                        sender.success("已将服务器网站地址标记为${args[2]}")
                        plugin.apply {
                            urlLooper = 0
                            looperDirection = true
                        }
                    }
                    "debug" -> {
                        if (args[2] == "true") {
                            Game.env.set("debug", true)
                            sender.success("已启动调试模式")
                        } else {
                            Game.env.set("debug", false)
                            sender.success("已关闭调试模式")
                        }
                    }
                    "ssHotReload" -> {
                        val content = "服务器脚本动态刷新，重载服务器脚本以生效"
                        if (args[2] == "true") {
                            Game.env.set("ssHotReload", true)
                            sender.success("已启用$content")
                        } else {
                            Game.env.set("ssHotReload", false)
                            sender.success("已关闭$content")
                        }
                    }
                    "survivalCenter" -> {
                        when {
                            args.size >= 5 -> {
                                val x = args[2].toDoubleOrNull()
                                val y = args[3].toDoubleOrNull()
                                val z = args[4].toDoubleOrNull()
                                var hasError = false
                                fun error(what: Char, v: String) {
                                    sender.error("参数${what}应为整数，却得到$v")
                                    hasError = true
                                }
                                if (x == null) error('x', args[2])
                                if (y == null) error('y', args[3])
                                if (z == null) error('z', args[4])
                                if (hasError) return true

                                Game.env.set("survivalCenter", Location(Base.surviveWorld, x!!, y!!, z!!))
                            }
                            sender is Player && args[2] == "here" -> {
                                Game.env.set("survivalCenter", sender.location.toBlockLocation())
                                sender.success("已将生存中心设为当前位置")
                            }
                            else -> {
                                sender.error("用法错误")
                                return true
                            }
                        }
                    }
                    else -> {
                        fun checkIntOverZero(): Boolean {
                            if (!args[2].isDigit()) {
                                sender.sendMessage(TextUtil.error("此变量只允许数字"))
                                return false
                            }
                            if (args[2].contains('.') || args[2].contains('-')) {
                                sender.sendMessage(TextUtil.error("此变量只允许正整数"))
                                return false
                            }
                            return true
                        }
                        if (!Game.varNames.contains(args[1])) {
                            sender.sendMessage(TextUtil.error("变量不存在"))
                            return true
                        }
                        if (checkIntOverZero()) {
                            Game.env.set(args[1], args[2].toIntOrNull())
                            sender.sendMessage(TextUtil.success("已将服务器环境变量${args[1]}设置为${args[2]}"))

                            plugin.apply {
                                reloadTask?.cancel()
                                saveTask?.cancel()
                                handleTasks()
                            }
                        }
                    }
                }
                Game.env.save(File(dataFolder, "env"))
            }
            args.first() == "notice" -> {
                if (args.size <= 1 && sender is Player && sender.isOp) {
                    Base.publicMsgPool.sendAllTo(sender.info() ?: return true)
                    return true
                }
                if (args.size < 3) {
                    sender.error("用法错误")
                    return true
                }
                when (args[1]) {
                    "append" -> {
                        val text = buildString {
                            for (i in 2 until args.size)
                                append(args[i] + ' ')
                            deleteCharAt(lastIndex)
                        }
                        Base.publicMsgPool.add(
                            text = text,
                            type = MessagePool.Type.Public,
                            extra = YamlConfiguration()
                        )
                        sender.success("已在公共消息池中追加该内容")
                        PlayerManager.forEachChatter { Base.publicMsgPool.sendUnreadTo(it) }
                    }
                    "remove" -> {
                        val id = args[2].toIntOrNull()
                        if (id == null || id < 0) {
                            sender.error("参数必须是自然数")
                            return true
                        }
                        Base.publicMsgPool.remove(id)
                        sender.success("已移除索引为${id}的消息")
                    }
                    "broadcast" -> PlayerManager.forEachChatter { Base.publicMsgPool.sendUnreadTo(it) }
                }
            }
            args.first() == "script" -> {
                val getter = sender.getter()
                if (!sender.isOp) {
                    sender.error(getter["command.error.permission"])
                    return true
                }
                if (args.size < 2 || args[1].isEmpty()) {
                    sender.error(getter["command.error.usage"])
                    return true
                }
                val src = buildString {
                    for (i in 1 until args.size) {
                        append(args[i] + ' ')
                    }
                    if (isNotEmpty()) {
                        deleteCharAt(lastIndex)
                    }
                }
                Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
                    var result: Any? = null
                    val time = measureTimeMillis {
                        result = Scripting.execute(
                            script = src,
                            output = if (sender is Player) sender.info()?.playerOutputStream else System.out
                        )
                    }
                    if (result == null) {
                        sender.error(getter["scripting.returnNull", time / 1000.0])
                    } else {
                        sender.success(getter["scripting.returnSomething", time / 1000.0, result.toString()])
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
        val first = args.first()

        if (args.size == 1) {
            val commands = mutableListOf("feedback", "about")
            if (sender.isOp) commands.addAll(listOf("stop", "reload", "set", "notice", "script"))
            return if (first.isEmpty()) {
                commands
            } else {
                commands.filter { it.startsWith(first) }.toMutableList()
            }
        } else {
            when (first) {
                "about" -> {
                    val sets = listOf(
                        Charsets.UTF_8.name(),
                        Charsets.UTF_16.name(),
                        Charsets.US_ASCII.name(),
                        Charsets.ISO_8859_1.name()
                    )
                    if (args.size == 1)
                        return sets.toMutableList()
                    else if (args.size == 2) {
                        val result = ArrayList<String>()
                        sets.forEach {
                            if (it.startsWith(args[1])) result.add(it)
                        }
                        return result.toMutableList()
                    }
                }
                "reload" -> {
                    if (!sender.isOp) {
                        return mutableListOf()
                    }
                    val models = listOf("game", "notice", "npc", "survey", "lang", "ss", "builder")
                    if (args.size == 1)
                        return models.toMutableList()
                    else if (args.size >= 2) {
                        val result = ArrayList<String>()
                        models.forEach { if (it.startsWith(args.last())) result.add(it) }
                        return result.toMutableList()
                    }
                }
                "set" -> {
                    if (!sender.isOp) {
                        return mutableListOf()
                    }

                    when (args.size) {
                        1 -> return Game.varNames.toMutableList()
                        2 -> {
                            val result = ArrayList<String>()
                            Game.varNames.forEach { if (it.startsWith(args[1])) result.add(it) }
                            return result.toMutableList()
                        }
                        3 -> {
                            when (args.last()) {
                                "debug" -> return mutableListOf("true", "false")
                                "survivalCenter" -> if (sender is Player) return mutableListOf("here")
                            }
                        }
                    }
                }
                "notice" -> {
                    if (!sender.isOp)
                        return mutableListOf()
                    val commands = mutableListOf("append", "remove", "broadcast")
                    if (args.size == 1) {
                        return if (args.first().isEmpty())
                            commands
                        else
                            commands.filter { it.startsWith(args.first()) }.toMutableList()
                    }
                }
            }
        }
        return mutableListOf()
    }
}