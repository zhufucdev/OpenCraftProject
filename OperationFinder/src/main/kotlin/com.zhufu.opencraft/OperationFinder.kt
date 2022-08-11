package com.zhufu.opencraft

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.zhufu.opencraft.listener.EveryThing
import com.zhufu.opencraft.operations.PlayerBlockOperation
import com.zhufu.opencraft.util.toErrorMessage
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toSuccessMessage
import com.zhufu.opencraft.util.toTipMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.StringReader
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class OperationFinder : JavaPlugin() {
    companion object {
        const val header = "§6§l-------以下是找到符合条件的操作-------"
        fun Location.toPrettyString(): String = "${world!!.name}($x,$y,$z)"
    }

    private val everyThing = EveryThing(this)
    override fun onEnable() {
        server.pluginManager.registerEvents(everyThing, this)
        everyThing.startMonitoring(60 * 20L)
    }

    override fun onDisable() {
        everyThing.stopMonitoring()
        OperationChecker.save()
    }


    private val format = SimpleDateFormat("yyyy/MM/dd/kk:mm:ss")
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "of") {
            if (args.isEmpty()) {
                sender.sendMessage("用法错误".toErrorMessage())
                return false
            }
            if (sender !is ConsoleCommandSender && !sender.isOp) {
                sender.sendMessage("您没有权限使用此命令".toErrorMessage())
                return true
            }
            when (args.first()) {
                "list" -> {
                    if (args.size == 1) {
                        sender.sendMessage(header)
                        OperationChecker.forEach { sender.sendMessage(it.toLocalMessage()) }
                    } else if (args.size == 2) {
                        sender.sendMessage("正在搜索".toInfoMessage())
                        val json: JsonObject
                        try {
                            json = JsonParser.parseString(args.last()).asJsonObject
                        } catch (e: Exception) {
                            e.printStackTrace()
                            sender.sendMessage(e.localizedMessage)
                            return true
                        }
                        var r = 0
                        val location: Location? = (
                                if (json.has("location")) {
                                    val l = json.getAsJsonObject("location")
                                    if (l.has("x") && l.has("y") && l.has("z")) {
                                        if (l.has("r")) {
                                            r = l["r"].asInt
                                        }
                                        Location(
                                            if (l.has("w")) Bukkit.getWorld(l["w"].asString) else {
                                                if (sender is Player) sender.location.world else {
                                                    sender.sendMessage("只有玩家才能使用此命令".toErrorMessage())
                                                    sender.sendMessage("如果是控制台命令，请使用\"w\"标签指定世界名称".toTipMessage())
                                                    return true
                                                }
                                            },
                                            l["x"].asDouble,
                                            l["y"].asDouble,
                                            l["z"].asDouble
                                        )
                                    } else if (l.has("r")) {
                                        r = l["r"].asInt
                                        if (sender is Player)
                                            sender.location
                                        else {
                                            sender.sendMessage("只有玩家才能使用此命令".toErrorMessage())
                                            return true
                                        }
                                    } else {
                                        sender.sendMessage("未知选择器:${args.last()}".toErrorMessage())
                                        return true
                                    }
                                } else null)
                            ?.also { sender.sendMessage("位置选择器: ${it.toPrettyString()},r=$r".toInfoMessage()) }

                        var specialSelector = ""
                        val type: OperationChecker.OperationType? = (if (json.has("type")) {
                            val value = json["type"].asString
                            if (!value.contains('/'))
                                OperationChecker.OperationType.valueOf(value.uppercase(Locale.getDefault()))
                            else {
                                val index = value.indexOf('/')
                                specialSelector = value.substring(index + 1).uppercase(Locale.getDefault())
                                if (specialSelector.isNotEmpty() && !Material.values()
                                        .any { it.name == specialSelector }
                                ) {
                                    sender.sendMessage("找不到方块: $specialSelector".toErrorMessage())
                                    return true
                                }

                                OperationChecker.OperationType.valueOf(
                                    value.substring(0, index)
                                        .uppercase(Locale.getDefault())
                                )
                            }
                        } else null)
                            ?.also { sender.sendMessage("种类选择器: ${it.name}    特殊参数: $specialSelector".toInfoMessage()) }

                        Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
                            val material = when {
                                type == OperationChecker.OperationType.BLOCK
                                        && try {
                                    Material.valueOf(specialSelector);false
                                } catch (e: IllegalArgumentException) {
                                    true
                                } -> return@runTaskAsynchronously

                                type == OperationChecker.OperationType.BLOCK -> Material.valueOf(specialSelector)
                                else -> null
                            }
                            OperationChecker.forEach {
                                val locationContent = (it.location != null && location != null
                                        && it.location!!.world == location.world
                                        && it.location!!.distance(location) <= r)
                                val typeContent = type == null || (it.operationType == type
                                        && if (type == OperationChecker.OperationType.BLOCK || specialSelector.isNotEmpty())
                                    (it as PlayerBlockOperation).block == material
                                else true)
                                val contentMap = listOf(
                                    Pair(location != null, locationContent),
                                    Pair(type != null, typeContent)
                                )

                                var isContent = true
                                for (k in contentMap) {
                                    if (k.first != k.second) {
                                        isContent = false
                                        break
                                    }
                                }
                                if (isContent)
                                    sender.sendMessage(it.toLocalMessage())
                            }
                            sender.sendMessage("完成".toSuccessMessage())
                        }
                    } else if (args.size >= 4) {
                        val from = format.parse(args[1])
                        val to = format.parse(args[3])
                        sender.sendMessage("正在搜索".toInfoMessage())
                        Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
                            OperationChecker[from, to].forEach {
                                sender.sendMessage(it.toLocalMessage())
                            }
                        }
                        sender.sendMessage("完成".toInfoMessage())
                    }
                }

                else -> {
                    val search = OperationChecker[args.first()]
                    if (search.isEmpty()) {
                        sender.sendMessage("未找到符合条件的记录".toInfoMessage())
                        return true
                    }
                    val r = ArrayList<String>()
                    search.forEach { r.add(it.toLocalMessage()) }
                    sender.sendMessage(header)
                    sender.sendMessage(*r.toTypedArray())
                }
            }
        }
        return true
    }

    /*
    Deprecated for its low performance
    override fun onTabComplete(sender: CommandSender?, command: Command?, alias: String?, args: Array<out String>?): MutableList<String> {
        if (command!!.name == "of") {
            val commands = arrayListOf("list")
            commands.addAll(OperationChecker.players())
            if (args!!.isEmpty()) {
                return commands.toMutableList()
            } else if (args.size == 1) {
                val r = ArrayList<String>()
                commands.forEach { if (it.startsWith(args.first())) r.add(it) }
                return r.toMutableList()
            }
        }
        return mutableListOf()
    }
    */
}