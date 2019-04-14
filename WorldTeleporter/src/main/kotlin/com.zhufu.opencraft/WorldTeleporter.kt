package com.zhufu.opencraft

import com.zhufu.opencraft.WorldTeleporter.mWorld.WorldPermissions.*
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import kotlin.collections.ArrayList

class WorldTeleporter : JavaPlugin(), PluginBase {
    private val customizedWorlds = ArrayList<String>()

    override fun onEnable() {
        Bukkit.getScheduler().runTaskLater(this, { _ ->
            config.getKeys(false).forEach {
                val world = Bukkit.getWorld(it)
                if (world == null) {
                    if (!File(it).exists()) {
                        logger.warning("World named $it doesn't exist. Excepting.")
                        config.set(it, null)
                    } else {
                        logger.info("World named $it isn't loaded, but exists on file. Loading...")
                        Bukkit.createWorld(WorldCreator.name(it))
                        customizedWorlds.add(it)
                    }
                }
            }
            saveConfig()
        }, 5 * 20)
    }

    override fun onDisable() {
        saveConfig()
        logger.info("Saving Customized Worlds...")
        customizedWorlds.forEach {
            logger.info("Saving for $it")
            server.getWorld(it)!!.save()
        }
    }

    class mWorld(val world: World, val per: WorldPermissions, val description: String = "") {
        enum class WorldPermissions {
            PUBLIC, PRIVATE, PROTECTED;

            fun canUse(sender: CommandSender) = (this == PUBLIC)
                    || (this == PRIVATE && (sender !is Player || sender.isOp))

            fun canSee(sender: CommandSender) = (this == PUBLIC)
                    || ((this == PRIVATE || this == PROTECTED) && (sender !is Player || sender.isOp))

            companion object {
                fun valueOf(value: String?, def: WorldPermissions): WorldPermissions = when {
                    value.isNullOrEmpty() -> def
                    value == "private" -> PRIVATE
                    value == "protected" -> PROTECTED
                    value == "public" -> PUBLIC
                    else -> def
                }
            }
        }
    }

    private fun getAvailableWorlds(): List<mWorld> {
        val r = ArrayList<mWorld>()
        Bukkit.getWorlds().forEach {
            if (it.name.startsWith("game_")){
                return@forEach
            }
            val per = config.getString("${it.name}.permission")
            val des = config.getString("${it.uid}.description")
            r.add(mWorld(it, mWorld.WorldPermissions.valueOf(per, PUBLIC), des ?: ""))
        }
        return r.toList()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "wt") {
            val getter = getLangGetter(if (sender is Player) PlayerManager.findInfoByPlayer(sender) else null)
            if (args.isEmpty()) {
                sender.sendMessage(TextUtil.error("用法错误"))
                return false
            } else if (args[0] == "list") {
                val p = sender
                val sb = ArrayList<String>()
                sb.add(TextUtil.info("-----以下是所有可能的世界-----") + "\n")
                getAvailableWorlds().forEach {
                    if (it.per.canSee(sender))
                        sb.add(
                                when (it.per) {
                                    PUBLIC -> TextUtil.getColoredText(">${it.world.name}", TextUtil.TextColor.BLUE, false, false)
                                    PRIVATE -> TextUtil.getColoredText("*${it.world.name}", TextUtil.TextColor.WHITE, true, false)
                                    PROTECTED -> TextUtil.getColoredText("!${it.world.name}", TextUtil.TextColor.GREY, true, false)
                                }
                        )
                }
                p.sendMessage(sb.toTypedArray())
            } else if (args[0] == "des" || args[0] == "set") {
                if (sender is Player && !(sender.isOp)) {
                    sender.sendMessage(TextUtil.error("您没有权限使用此命令"))
                    return false
                }
                if (args.size < 3) {
                    sender.sendMessage(TextUtil.error("参数错误"))
                    return false
                }
                val world: World? = Bukkit.getWorld(args[1])
                if (world == null) {
                    sender.sendMessage(TextUtil.error("世界不存在"))
                    return false
                }

                if (args[0] == "des") {
                    val description = StringBuilder()
                    for (i in 2 until args.size) {
                        description.append(args[i])
                        description.append(' ')
                    }
                    description.deleteCharAt(description.length - 1)
                    config.set("${world.name}.description", description.toString())
                    sender.sendMessage(TextUtil.info("成功将世界介绍更改为\"$description\""))
                } else {
                    if (args[2] != "public" && args[2] != "private" && args[2] != "protected") {
                        sender.sendMessage(TextUtil.error("未知权限: ${args[2]},使用/help wt set 查看帮助"))
                        return true
                    }
                    config.set("${world.name}.permission", args[2])
                    sender.sendMessage(TextUtil.info("成功将世界权限更改为\"${args[2]}\""))
                }
                config.save(File(dataFolder, "worlds.yml"))
            } else if (args.first() == "new") {
                if (!sender.isOp) {
                    sender.error(getter["command.error.permission"])
                }
                if (args.size < 3) {
                    sender.error(getter["command.error.usage"])
                    return true
                }
                val type = if (WorldType.values().any { it.name == args[1].toUpperCase() }) WorldType.valueOf(args[1].toUpperCase()) else null
                val name = args[2]
                if (type == null) {
                    sender.error("Not such type ${args[1]}")
                    return true
                }
                Bukkit.createWorld(WorldCreator(name).type(type)).also {
                    config.set("${it!!.name}.permission", "public")
                }
                customizedWorlds.add(name)
                sender.success("Succeed.")
            } else if (args.first() == "unload") {
                if (!sender.isOp) {
                    sender.error(getter["command.error.permission"])
                }
                if (args.size < 2) {
                    sender.error(getter["command.error.usage"])
                    return true
                }
                val world = if (server.worlds.any { it.name == args[1] }) server.getWorld(args[1]) else null
                if (world == null) {
                    sender.error("No such world ${args[1]}")
                    return true
                }
                world.save()
                customizedWorlds.remove(args[1])
                server.unloadWorld(world, true)
                sender.success("Succeed.")
            } else if (args.isNotEmpty()) {
                if (!(sender is Player)) {
                    sender.sendMessage("此命令只能被玩家所运行")
                    return true
                }
                val p = sender
                val world: World? = Bukkit.getWorld(args[0])
                if (world != null) {
                    val per = config.getString("${world.name}.permission")
                    if (mWorld.WorldPermissions.valueOf(per, def = PUBLIC).canUse(sender)) {
                        val event = PlayerTeleportedEvent(p, p.location, world.spawnLocation)
                        Bukkit.getPluginManager().callEvent(event)
                        Bukkit.getScheduler().runTaskLater(this, { _ ->
                            if (!event.isCancelled)
                                p.teleport(world.spawnLocation)
                        }, 20)
                    } else {
                        p.sendMessage(TextUtil.error("您没有访问此世界的权限"))
                        p.sendMessage(TextUtil.tip("使用/wt list 查看可能的世界"))
                    }
                } else {
                    sender.sendMessage(TextUtil.error("世界不存在"))
                }
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        fun hasPermission(sender: CommandSender) = sender is ConsoleCommandSender || (sender is Player && sender.isOp)
        fun showPermissions(present: String): MutableList<String> {
            val permissions = listOf("private", "public", "protected")
            val results = ArrayList<String>()
            permissions.forEach {
                when {
                    it.startsWith(present) -> results.add(it)
                    present == "" -> results.add(it)
                }
            }
            return results.toMutableList()
        }
        if (command.name == "wt") {
            if (args.isNotEmpty()) {
                val first = args.first()
                val worlds = MutableList(server.worlds.size) { server.worlds[it].name }
                when (first) {
                    "set" -> {
                        if (!hasPermission(sender)) {
                            sender.sendMessage(TextUtil.error("您没有权限使用此命令"))
                            return mutableListOf()
                        }
                        if (args.size == 1) {
                            return worlds
                        } else if (args.size == 2) {
                            val results = ArrayList<String>()
                            var isCompleted = false
                            worlds.forEach {
                                if (it == args[1]) {
                                    isCompleted = true
                                    return@forEach
                                }
                                if (it.startsWith(args[1])) {
                                    results.add(it)
                                }
                            }
                            if (!isCompleted) return results.toMutableList()
                            else return showPermissions("")
                        } else if (args.size == 3) {
                            return showPermissions(args[2])
                        } else sender.sendMessage(arrayOf(TextUtil.error("用法错误"), server.getPluginCommand("wt set")!!.usage))
                    }
                    "des" -> {
                        if (!hasPermission(sender)) {
                            sender.sendMessage(TextUtil.error("您没有权限使用此命令"))
                            return mutableListOf()
                        }
                        if (args.size == 2) {
                            return worlds
                        } else {
                            sender.sendMessage(server.getPluginCommand("wt des")!!.usage)
                        }
                    }
                    "new" -> {
                        if (!hasPermission(sender)) {
                            return mutableListOf()
                        }
                        if (args.size == 2) {
                            val types = ArrayList<String>()
                            WorldType.values().forEach {
                                types.add(it.name)
                            }
                            if (args[1].isEmpty()) {
                                return types
                            } else {
                                return types.asSequence().filter { it.startsWith(args[1]) }.toMutableList()
                            }
                        }
                    }
                    "unload" -> {
                        if (!hasPermission(sender)) {
                            return mutableListOf()
                        }
                        if (args.size == 2) {
                            if (args[1].isEmpty()) {
                                return worlds
                            } else {
                                return worlds.asSequence().filter { it.startsWith(args[1]) }.toMutableList()
                            }
                        }
                    }
                    else -> {
                        val r = ArrayList<String>()
                        val t = ArrayList<String>()
                        t.add("list")
                        if (hasPermission(sender)) {
                            t.add("set")
                            t.add("des")
                            t.add("new")
                            t.add("unload")
                        }
                        getAvailableWorlds().forEach {
                            if (it.per.canUse(sender))
                                t.add(it.world.name)
                        }
                        return if (first.isNotEmpty()) {
                            t.forEach { if (it.startsWith(first)) r.add(it) }
                            r.toMutableList()
                        } else t.toMutableList()
                    }
                }
            }
        }
        return mutableListOf()
    }
}