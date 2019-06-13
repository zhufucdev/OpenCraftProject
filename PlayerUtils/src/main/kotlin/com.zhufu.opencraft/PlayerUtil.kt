package com.zhufu.opencraft

import com.zhufu.opencraft.special_items.FlyWand
import com.zhufu.opencraft.special_items.Portal
import com.zhufu.opencraft.special_items.SpecialItem
import com.zhufu.opencraft.ui.MenuInterface
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.graalvm.polyglot.Context

class PlayerUtil : JavaPlugin() {
    companion object {
        val selected = HashMap<Player, Nameable>()
    }

    override fun onEnable() {
        Everything.init(this)
        PortalHandler.init(this)

    }

    override fun onDisable() {
        Everything.onServerClose()
        PortalHandler.onServerClose()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val getter = sender.lang()
        if (command.name == "lu") {
            if (!sender.isOp) {
                sender.error(getter["command.error.permission"])
                return true
            }
            if (sender !is Player) {
                sender.error(getter["command.error.playOnly"])
                return true
            }
            if (args.size < 7) {
                sender.error(getter["command.error.usage"])
                return false
            }
            val x1 = args[1].toIntOrNull()
            val y1 = args[2].toIntOrNull()
            val z1 = args[3].toIntOrNull()
            val x2 = args[4].toIntOrNull()
            val y2 = args[5].toIntOrNull()
            val z2 = args[6].toIntOrNull()
            if (x1 == null || y1 == null || z1 == null || x2 == null || y2 == null || z2 == null) {
                sender.sendMessage(TextUtil.error("坐标值必须为整数"))
                return true
            }
            val from = Location(sender.world, x1.toDouble(), y1.toDouble(), z1.toDouble())
            val to = Location(sender.world, x2.toDouble(), y2.toDouble(), z2.toDouble())
            when (args.first()) {
                "DTWB" -> {
                    Everything.createDTWB(from, to)
                }
                "RP" -> {

                }
                "TP" -> {
                    Everything.createTP(from, to)
                }
                else -> {
                    sender.error(getLang(sender, "command.error.usage"))
                    return false
                }
            }
        } else if (command.name == "pu") {
            if (sender !is Player) {
                sender.error(getter["command.error.playerOnly"])
                return true
            }
            if (args.isEmpty()) {
                sender.error(getter["command.error.usage"])
                return true
            }
            when (args.first()) {
                "menu" -> MenuInterface(this, sender).show(sender)
                "rename" -> {
                    if (!selected.containsKey(sender)) {
                        sender.error(getter["pu.error.unselected"])
                    } else {
                        if (args.size < 2) {
                            sender.error(getter["command.error.usage"])
                        } else {
                            val info = sender.info()
                            if (info == null) {
                                sender.error(getter["player.error.unknown"])
                            } else {
                                val checkpoint = selected[sender]!!
                                val oldName = checkpoint.name
                                val newName = args[1]
                                checkpoint.name = newName
                                sender.success(getter["ui.checkpoint.rename.done", oldName, newName])
                            }
                        }
                    }
                }
                "server:markMessageRead" -> {
                    if (args.size >= 2) {
                        val index = args[1].toIntOrNull() ?: return true
                        var r = false
                        if (args.size >= 3) {
                            if (args[2] == "public") {
                                r = Base.publicMsgPool.markAsRead(index, sender.info() ?: return true)
                            }
                        } else {
                            r = sender.info()?.messagePool?.markAsRead(index) == true
                        }
                        if (r)
                            sender.success(getter["msg.markAsRead"])
                        else
                            sender.warn(getter["msg.alreadyRead"])
                    }
                }
                "si" -> {
                    if (args.size < 4) {
                        sender.error(getter["command.error.toFewArgs", 3])
                        return true
                    } else if (args[1] != "give") {
                        sender.error(getter["command.error.usage"])
                        return true
                    }

                    val player = Bukkit.getPlayer(args[2])
                    if (player == null) {
                        sender.error(getter["command.error.playerNotFound"])
                        return true
                    }
                    val amount = if (args.size == 4) 1 else {
                        val i = args[4].toIntOrNull()
                        if (i == null) {
                            sender.error(getter["command.error.argNonDigit"])
                            return true
                        }
                        i
                    }
                    val itemName = when {
                        args[3].equals(SpecialItem.Type.FlyingWand.name, true) -> {
                            player.inventory.addItem(FlyWand(getter).apply { this.amount = amount })
                            getter["wand.name"]
                        }
                        args[3].equals(SpecialItem.Type.Portal.name, true) -> {
                            player.inventory.addItem(Portal(getter).apply { this.amount = amount })
                            getter["portal.name"]
                        }
                        else -> {
                            sender.error(getter["command.error.noSuchItem", args[3]])
                            return true
                        }
                    }
                    sender.success(getter["command.done"])
                    if (player != sender) {
                        player.info(getter["si.given", itemName, amount])
                    }
                }
                "script" -> {
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
                    Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
                        val info = sender.info()
                        if (info == null) {
                            sender.error(getter["command.error.unknown"])
                            return@runTaskAsynchronously
                        }
                        val timeBegin = System.currentTimeMillis()
                        val result = Scripting.evalLineAs(info, src, "Chat")
                        val timeEnd = System.currentTimeMillis()
                        if (result == null) {
                            sender.error(getter["scripting.returnNull", (timeEnd - timeBegin) / 1000.0])
                        } else {
                            sender.success(getter["scripting.returnSomething", (timeEnd - timeBegin) / 1000.0, result.toString()])
                        }
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
        if (command.name == "lu") {
            if (!sender.isOp || sender !is Player) {
                return mutableListOf()
            }
            val commands = mutableListOf("DTWB", "RP", "TP")
            when (args.size) {
                1 -> {
                    return if (args.first().isEmpty()) {
                        commands
                    } else {
                        val r = ArrayList<String>()
                        commands.forEach {
                            if (it.startsWith(args.first()))
                                r.add(it)
                        }
                        r
                    }
                }
                else -> {
                    if (!commands.contains(args.first())) {
                        return mutableListOf()
                    }
                    val target = sender.getTargetBlock(null, 5)
                    return mutableListOf(
                        when (args.size) {
                            2 -> target.x.toString()
                            3 -> target.y.toString()
                            4 -> target.z.toString()
                            5 -> target.x.toString()
                            6 -> target.y.toString()
                            7 -> target.z.toString()
                            else -> ""
                        }
                    )
                }
            }
        } else {
            if (args.size == 1) {
                val all = mutableListOf("rename", "menu", "script").apply { if (sender.isOp) add("si") }
                return if (args.first().isEmpty())
                    all
                else {
                    all.filter { it.startsWith(args.first()) }.toMutableList()
                }
            } else if (args.first() == "si") when (args.size) {
                2 -> {
                    return mutableListOf("give")
                }
                3 -> {
                    val r = mutableListOf<String>()
                    if (args[2].isEmpty()) {
                        Bukkit.getOnlinePlayers().forEach { r.add(it.name) }
                    } else {
                        Bukkit.getOnlinePlayers().forEach {
                            if (it.name.startsWith(args[2]))
                                r.add(it.name)
                        }
                    }
                    return r
                }
                4 -> {
                    val r = mutableListOf<String>()
                    if (args[3].isEmpty()) {
                        SpecialItem.Type.values().forEach { r.add(it.name) }
                    } else {
                        SpecialItem.Type.values().forEach {
                            if (it.name.startsWith(args[3], true))
                                r.add(it.name)
                        }
                    }
                    return r
                }
            }
        }
        return mutableListOf()
    }
}