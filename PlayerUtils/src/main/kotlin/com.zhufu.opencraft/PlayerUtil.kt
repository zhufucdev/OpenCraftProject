package com.zhufu.opencraft

import com.zhufu.opencraft.lobby.PlayerLobby
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import com.zhufu.opencraft.special_item.dynamic.SpecialItem
import com.zhufu.opencraft.special_item.static.WrappedItem
import com.zhufu.opencraft.ui.LobbyVisitor
import com.zhufu.opencraft.ui.MenuInterface
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class PlayerUtil : JavaPlugin() {
    companion object {
        val selected = HashMap<Player, Nameable>()
    }

    override fun onEnable() {
        Everything.init(this)
        ChartHandler.init(this)

        ServerCaller["SolveLobbyVisitor"] = {
            val info = (it.firstOrNull()
                ?: throw IllegalArgumentException("This call must be give at least one Info parameter.")) as Info
            info.player.sendActionText(info.getter()["ui.visitor.booting"].toInfoMessage())
            Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
                val ui = LobbyVisitor(this, info)
                Bukkit.getScheduler().callSyncMethod(this) {
                    ui.show(info.player)
                }
            }
        }
    }

    override fun onDisable() {
        Everything.onServerClose()
        ChartHandler.cleanUp()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val getter = sender.getter()
        if (command.name == "lu") {
            if (!sender.isOp) {
                sender.error(getter["command.error.permission"])
                return true
            }
            if (sender !is Player) {
                sender.error(getter["command.error.playOnly"])
                return true
            }
            if (args.isNotEmpty()) {
                if (args.first() == "CRT") {
                    Everything.createCRT(sender.location)
                }
            } else {
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
                "menu" -> {
                    if (sender.info()?.isLogin == true)
                        MenuInterface(this, sender).show(sender)
                    else
                        sender.error(getter["user.error.notLoginYet"])
                }
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
                                sender.success(getter["ui.renamed", oldName, newName])
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
                "messages" -> {
                    val info = sender.info()
                    if (info == null) {
                        sender.error(getter["player.error.unknown"])
                    } else {
                        sender.info(getter["msg.list"])
                        if (!info.messagePool.isEmpty)
                            info.messagePool.sendAllTo(info)
                        else
                            sender.error(getter["msg.empty"])
                    }
                }
                "si" -> {
                    if (!sender.isOp) {
                        sender.error(getter["command.error.permission"])
                        return true
                    }

                    if (args.size < 4) {
                        sender.error(getter["command.error.tooFewArgs", 3])
                        return true
                    }
                    if (args[1] != "give") {
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
                    val arguments = arrayListOf<Any?>()
                    if (args.size > 5) {
                        val script = buildString {
                            for (i in 5 until args.size) {
                                append(args[i])
                                append(' ')
                            }
                            deleteCharAt(lastIndex)
                        }
                        script.split(';').forEach {
                            try {
                                arguments.add(Scripting.execute(it))
                            } catch (e: Exception) {
                                sender.error("${e::class.qualifiedName}: ${e.message} at argument $it.")
                                return true
                            }
                        }
                    }
                    var t: Any? = null
                    fun asSI(name: String) {
                        t = SpecialItem.make(name, amount, player, *arguments.toArray())
                    }
                    fun asWrapped(name: String) {
                        t = WrappedItem.make(name, amount, player, *arguments.toArray())
                    }

                    when {
                        args[3].startsWith("static/") -> {
                            asWrapped(args[3].substring(7))
                        }
                        SpecialItem.types.contains(args[3]) -> {
                            asSI(args[3])
                        }
                        else -> {
                            asWrapped(args[3])
                        }
                    }

                    val item = t
                    if (item == null) {
                        sender.error(getter["command.error.noSuchItem", args[3]])
                        return true
                    }
                    when (item) {
                        is SpecialItem -> player.inventory.addItem(item.itemLocation.itemStack)
                        is WrappedItem -> player.inventory.addItem(item)
                        else -> throw UnsupportedOperationException("Using ${item::class.simpleName} as an item stack.")
                    }
                    sender.success(getter["command.done"])
                    if (player != sender) {
                        player.info(getter["si.given", args[3], amount])
                    }
                }
                "exp" -> {
                    if (!sender.isOp) {
                        sender.error(getter["command.error.permission"])
                        return true
                    }

                    if (args.size < 3) {
                        sender.error(getter["command.error.tooFewArgs", 3])
                        return true
                    }
                    val player = PlayerManager.findOfflineInfoByName(args[1])
                    if (player == null) {
                        sender.error(getter["player.error.notFound", args[1]])
                        return true
                    }
                    if (args.size == 2) {
                        sender.info(getter["rpg.command.info", player.name, player.exp])
                    } else if (args.size >= 4) {
                        when (args[2]) {
                            "add" -> {
                                val amount = args[3].toIntOrNull()
                                if (amount == null) {
                                    sender.error(getter["command.error.argNotDigit"])
                                    return true
                                }
                                player.exp += amount
                                sender.success(getter["rpg.command.give", player.name, amount])
                            }
                            "set" -> {
                                val amount = args[3].toIntOrNull()
                                if (amount == null) {
                                    sender.error(getter["command.error.argNotDigit"])
                                    return true
                                }
                                player.exp = amount
                                sender.success(getter["rpg.command.set", player.name, amount])
                            }
                            else -> sender.error(getter["command.error.usage"])
                        }
                    }
                }
            }
        } else if (command.name == "lobby") {
            fun printStatics(lobby: PlayerLobby) {
                sender.apply {
                    val header = "lobby.statics"
                    tip(getter["$header.title", lobby.owner.name])
                    var likes = 0
                    var dislikes = 0
                    var gatherBuilders = 0
                    val playersLiking = StringBuilder()
                    val playerDisliking = StringBuilder()
                    val gatherBuilding = StringBuilder()
                    lobby.reviews().forEach {
                        if (it.second) {
                            likes++
                            playersLiking.append(it.first + ", ")
                        } else {
                            dislikes++
                            playerDisliking.append(it.first + ", ")
                        }
                    }
                    lobby.partners().forEach {
                        gatherBuilders++
                        gatherBuilding.append("$it, ")
                    }
                    info(getter["lobby.get.views", lobby.views])
                    info(
                        if (likes == 0)
                            getter["$header.like.empty"]
                        else
                            getter["$header.like.content", playersLiking.removeSuffix(", "), likes]
                    )
                    info(
                        if (dislikes == 0)
                            getter["$header.dislike.empty"]
                        else
                            getter["$header.dislike.content", playerDisliking.removeSuffix(", "), dislikes]
                    )
                    if (sender !is Player || lobby.owner.uuid == sender.uniqueId) {
                        info(
                            if (gatherBuilders == 0)
                                getter["$header.gather.empty"]
                            else
                                getter["$header.gather.content", gatherBuilding.removeSuffix(", "), gatherBuilders]
                        )
                    }
                }
            }

            fun printUsage() = sender.tip(getCommand("lobby")!!.usage)
            when (args.size) {
                0 -> {
                    if (sender !is Player)
                        sender.error(getter["command.error.playerOnly"])
                    else {
                        val info = sender.info()
                        if (info == null) {
                            sender.error(getter["player.error.unknown"])
                        } else {
                            printStatics(PlayerLobbyManager[info])
                        }
                    }
                }
                1 -> {
                    val target = OfflineInfo.findByName(args.first())
                    if (target == null) {
                        sender.error(getter["player.error.notFound", args.first()])
                    } else {
                        printStatics(PlayerLobbyManager[target])
                    }
                }
                2 -> {
                    if (sender !is Player) {
                        sender.error(getter["command.error.playerOnly"])
                        return true
                    }
                    val target = OfflineInfo.findByName(args.first())
                    if (target == null) {
                        sender.error(getter["player.error.notFound", args.first()])
                    } else {
                        when (args.last()) {
                            "go" -> {
                                PlayerLobbyManager[target].tpHere(sender)
                            }
                            else -> {
                                val info = sender.info()
                                if (info == null) {
                                    sender.error(getter["player.error.unknown"])
                                } else {
                                    val lobby = PlayerLobbyManager[info]
                                    when (args.last()) {
                                        "permit" -> {
                                            if (lobby.addPartner(target))
                                                sender.success(getter["lobby.permitted", target.name])
                                            else
                                                sender.error(getter["lobby.error.alreadyPermitted", target.name])
                                        }
                                        "forbid" -> {
                                            if (lobby.removePartner(target))
                                                sender.success(getter["lobby.forbid", target.name])
                                            else
                                                sender.error(getter["lobby.error.alreadyForbidden", target.name])
                                        }
                                        else -> {
                                            sender.error(getter["command.error.usage"])
                                            printUsage()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    sender.error(getter["command.error.usage"])
                    printUsage()
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
        } else if (command.name == "pu") {
            fun getPlayerList(input: String, onlineOnly: Boolean = true): MutableList<String> {
                val r = mutableListOf<String>()
                if (input.isEmpty()) {
                    if (onlineOnly)
                        Bukkit.getOnlinePlayers().forEach { r.add(it.name) }
                    else
                        Bukkit.getOfflinePlayers().forEach { r.add(it.name ?: return@forEach) }
                } else {
                    val selector = { it: OfflinePlayer ->
                        if (it.name?.startsWith(args[2]) == true)
                            r.add(it.name!!)
                    }
                    if (onlineOnly) Bukkit.getOnlinePlayers().forEach(selector)
                    else Bukkit.getOfflinePlayers().forEach(selector)
                }
                return r
            }
            when {
                args.size == 1 -> {
                    val all = mutableListOf("rename", "menu", "message", "exp").apply { if (sender.isOp) add("si") }
                    return if (args.first().isEmpty())
                        all
                    else {
                        all.filter { it.startsWith(args.first()) }.toMutableList()
                    }
                }
                args.first() == "si" -> when (args.size) {
                    2 -> {
                        return mutableListOf("give")
                    }
                    3 -> {
                        return getPlayerList(args[2])
                    }
                    4 -> {
                        val r = mutableListOf<String>()
                        if (args[3].isEmpty()) {
                            SpecialItem.types.forEach { r.add(it) }
                        } else {
                            SpecialItem.types.forEach {
                                if (it.startsWith(args[3], true))
                                    r.add(it)
                            }
                        }
                        return r
                    }
                }
                args.first() == "exp" -> when (args.size) {
                    2 -> {
                        return getPlayerList(args[1], false)
                    }
                    3 -> {
                        val c = mutableListOf("add", "set")
                        return if (args[2].isEmpty()) {
                            c
                        } else {
                            c.filter { it.startsWith(args[2], true) }.toMutableList()
                        }
                    }
                }
            }
        } else if (command.name == "lobby") {
            when (args.size) {
                1 -> {
                    val players = mutableListOf<String>().apply {
                        PlayerLobbyManager.list().forEach {
                            add(it.owner.name ?: return@forEach)
                        }
                    }
                    return if (args.first().isEmpty()) players
                    else players.filter { it.startsWith(args.first()) }.toMutableList()
                }
                2 -> {
                    if (sender is Player) {
                        val target = OfflineInfo.findByName(args.first())
                        if (target != null) {
                            val commands = mutableListOf("go")
                            if (target.uuid != sender.uniqueId)
                                commands.addAll(listOf("permit", "forbid"))

                            return if (args.last().isEmpty()) commands
                            else commands.filter { it.startsWith(args.last()) }.toMutableList()
                        }
                    }
                }
            }
        }
        return mutableListOf()
    }
}