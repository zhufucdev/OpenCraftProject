package com.zhufu.opencraft

import com.zhufu.opencraft.ui.MenuInterface
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class PlayerUtil : JavaPlugin(),PluginBase {
    companion object {
        val selected = HashMap<Player,Int>()
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
        if (command.name == "lu"){
            if (!sender.isOp){
                sender.sendMessage(TextUtil.error("您没有权限使用此命令"))
                return true
            }
            if (sender !is Player){
                sender.sendMessage(TextUtil.error("只有玩家才能使用此命令"))
                return true
            }
            if (args.size < 7){
                sender.sendMessage(TextUtil.error("用法错误"))
                return false
            }
            val x1 = args[1].toIntOrNull()
            val y1 = args[2].toIntOrNull()
            val z1 = args[3].toIntOrNull()
            val x2 = args[4].toIntOrNull()
            val y2 = args[5].toIntOrNull()
            val z2 = args[6].toIntOrNull()
            if (x1 == null || y1 == null || z1 == null || x2 == null || y2 == null || z2 == null){
                sender.sendMessage(TextUtil.error("坐标值必须为整数"))
                return true
            }
            val from = Location(sender.world, x1.toDouble(), y1.toDouble(), z1.toDouble())
            val to = Location(sender.world, x2.toDouble(), y2.toDouble(), z2.toDouble())
            when (args.first()){
                "DTWB" -> {
                    Everything.createDTWB(from, to)
                }
                "RP" -> {

                }
                "TP" -> {
                    Everything.createTP(from, to)
                }
                else -> {
                    sender.sendMessage(TextUtil.error("用法错误"))
                    return false
                }
            }
        } else if (command.name == "pu"){
            if (sender !is Player){
                sender.error(getLang(sender,"command.error.playerOnly"))
                return true
            }
            if (args.isEmpty()){
                sender.error(getLang(sender,"command.error.usage"))
                return true
            }
            when (args.first()){
                "menu" -> MenuInterface(this,sender).show(sender)
                "rename" -> {
                    if (!selected.containsKey(sender)){
                        sender.error(getLang(sender,"pu.error.unselected"))
                    } else {
                        if (args.size < 2){
                            sender.error(getLang(sender,"command.error.usage"))
                        } else {
                            val info = sender.info()
                            if (info == null){
                                sender.error(Language.getDefault("player.error.unknown"))
                            } else {
                                val index = info.checkpoints[selected[sender]!!]
                                val oldName = index.id
                                val newName = args[1]
                                index.id = newName
                                sender.success(getLang(sender,"ui.checkpoint.rename.done",oldName,newName))
                            }
                        }
                    }
                }
                "server:markMessageRead" -> {
                    if (args.size >= 2){
                        val index = args[1].toIntOrNull()?:return true
                        var r = false
                        if (args.size >= 3){
                            if (args[2] == "public"){
                                r = Base.publicMsgPool.markAsRead(index,sender.info()?:return true)
                            }
                        } else {
                            r = sender.info()?.messagePool?.markAsRead(index) == true
                        }
                        if (r)
                            sender.success(getLang(sender,"msg.markAsRead"))
                        else
                            sender.warn(getLang(sender,"msg.alreadyRead"))
                    }
                }
            }
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        if (!sender.isOp || sender !is Player){
            return mutableListOf()
        }
        if (command.name == "lu"){
            val commands = mutableListOf("DTWB","RP","TP")
            when (args.size){
                1 -> {
                    return if (args.first().isEmpty()){
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
                    if (!commands.contains(args.first())){
                        return mutableListOf()
                    }
                    val target = sender.getTargetBlock(null,5)
                    return mutableListOf(when(args.size){
                        2 -> target.x.toString()
                        3 -> target.y.toString()
                        4 -> target.z.toString()
                        5 -> target.x.toString()
                        6 -> target.y.toString()
                        7 -> target.z.toString()
                        else -> ""
                    })
                }
            }
        } else {
            if (args.size == 1) {
                return mutableListOf("rename")
            }
        }
        return mutableListOf()
    }
}