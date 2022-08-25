package com.zhufu.opencraft

import com.zhufu.opencraft.api.Nameable
import com.zhufu.opencraft.api.ServerCaller
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.data.OfflineInfo
import com.zhufu.opencraft.lobby.PlayerLobby
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import com.zhufu.opencraft.player_community.PublicMessagePool
import com.zhufu.opencraft.special_item.*
import com.zhufu.opencraft.ui.LobbyVisitor
import com.zhufu.opencraft.ui.MenuInterface
import com.zhufu.opencraft.ui.ReverseCraftingTableInventory
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toSuccessMessage
import org.bukkit.Bukkit
import org.bukkit.Location
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
        PortalHandler.init(this)
        ChartHandler.init(this)

        ServerCaller["SolveLobbyVisitor"] = {
            val info = (it.firstOrNull()
                ?: throw IllegalArgumentException("This call must be give at least one Info parameter.")) as Info
            info.player.sendActionBar(info.getter()["ui.visitor.booting"].toInfoMessage())
            Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
                val ui = LobbyVisitor(this, info)
                Bukkit.getScheduler().callSyncMethod(this) {
                    ui.show(info.player)
                }
            }
        }
        ServerCaller["SolveRCT"] = {
            val player = (it.firstOrNull()
                ?: throw IllegalArgumentException("This call must be given at least one Player parameter.")) as Player
            ReverseCraftingTableInventory(player.getter(), this).show(player)
        }
    }

    override fun onDisable() {
        Everything.onServerClose()
        PortalHandler.onServerClose()
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
                    sender.error("坐标值必须为整数")
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
                        val id = args[1].toIntOrNull() ?: return true
                        var r = false
                        if (args.size >= 3) {
                            if (args[2] == "public") {
                                r = PublicMessagePool.markAsRead(id, sender.info() ?: return true)
                            }
                        } else {
                            r = sender.info()?.messagePool?.markAsRead(id) == true
                        }
                        if (r)
                            sender.sendActionBar(getter["msg.markAsRead"].toSuccessMessage())
                        else
                            sender.sendActionBar(getter["msg.alreadyRead"].toSuccessMessage())
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
                    if (args.size < 4) {
                        sender.error(getter["command.error.tooFewArgs", 3])
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
                    val item = StatefulSpecialItem.make(args[3], amount, player)
                    if (item == null) {
                        sender.error(getter["command.error.noSuchItem", args[3]])
                        return true
                    }
                    player.inventory.addItem(item)
                    sender.success(getter["command.done"])
                    if (player != sender) {
                        player.info(getter["si.given", args[3], amount])
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
                        val info = sender.info()
                        if (info == null) {
                            sender.error(getter["player.error.unknown"])
                            return true
                        }
                        when (args.last()) {
                            "go" -> {
                                if (info.isLogin)
                                    info.logout()
                                PlayerLobbyManager[target].visitBy(sender)
                            }
                            else -> {
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
            if (args.size == 1) {
                val all = mutableListOf("rename", "menu", "script", "message").apply { if (sender.isOp) add("si") }
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
                        StatefulSpecialItem.types.forEach { r.add(it) }
                    } else {
                        StatefulSpecialItem.types.forEach {
                            if (it.startsWith(args[3], true))
                                r.add(it)
                        }
                    }
                    return r
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