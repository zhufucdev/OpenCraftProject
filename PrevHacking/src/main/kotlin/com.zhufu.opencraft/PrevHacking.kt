package com.zhufu.opencraft

import com.zhufu.opencraft.util.toErrorMessage
import com.zhufu.opencraft.util.toInfoMessage
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

class PrevHacking : JavaPlugin(), org.bukkit.event.Listener {
    class MyPlayer(val player: Player){
        var isHacking: Int = 0
        private set
        var lastHackTime: Long = 0
        private set
        var lastBlockBroken: Location = player.location
        var lastOrePointed: Location? = null
        val blockToBeFilled = ArrayList<Block>()
        var hasBrokenBlockFarther: Boolean = false
        fun setIsHacking(isHacking: Int){
            this.isHacking = isHacking
            if (showPlayerHackingAlert)
                println("[PrevHacking] ${player.name} seemed to hack!")

            if (System.currentTimeMillis() - lastHackTime > 2*60*1000L && hasBrokenBlockFarther)
                this.isHacking = 0

            if (isHacking > threshold){
                player.teleport(blockToBeFilled.first().location.add(Vector(0,1,0)))
                blockToBeFilled.forEach { it.type = Material.STONE }
                player.kick("服务器不允许作弊!".toErrorMessage())
            }
            else {
                lastHackTime = System.currentTimeMillis()
                hasBrokenBlockFarther = false
            }
        }
    }
    companion object {
        lateinit var mPlugin: JavaPlugin
        val playerList = ArrayList<MyPlayer>()
        private val config: FileConfiguration
            get() = mPlugin.config
        var threshold: Int
            get() = config.getInt("threshold",30)
            set(value) = config.set("threshold",value)
        var showPlayerOrePointInfo: Boolean
            get() = config.getBoolean("showPOI",false)
            set(value) = config.set("showPOI",value)
        var showPlayerHackingAlert: Boolean
            get() = config.getBoolean("showPHA",true)
            set(value) = config.set("showPHA",value)
    }
    override fun onEnable() {
        mPlugin = this
        server.onlinePlayers.forEach {
            playerList.add(MyPlayer(it))
        }
        server.pluginManager.registerEvents(this,this)
        server.pluginManager.registerEvents(Listener,this)
    }

    override fun onDisable() {
        saveConfig()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "ph"){
            if (!sender.isOp){
                sender.sendMessage("您没有权限使用此命令".toErrorMessage())
                return true
            }
            if (args.size < 3 || args.first() != "set"){
                sender.sendMessage("用法错误".toErrorMessage())
                return false
            }
            val num = args[2].toIntOrNull()
            if (num == null){
                sender.sendMessage("无效参数: ${args[2]}: 参数不是自然数".toErrorMessage())
                return true
            }

            when (args[1]){
                "threshold" -> threshold = num
                "showPlayerOrePointingInfo" -> showPlayerOrePointInfo = num == 1
                "showPlayerHackingAlert" -> showPlayerHackingAlert = num == 1
                else -> {
                    sender.sendMessage("未知参数: ${args[1]}".toErrorMessage())
                    return true
                }
            }
            sender.sendMessage("${args[1]}已被更新为${args[2]}".toInfoMessage())
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        if (command.name == "ph"){
            if (sender.isOp){
                return when (args.size){
                    1 -> mutableListOf("set")
                    2 -> {
                        val arg = mutableListOf("threshold","showPlayerOrePointingInfo","showPlayerHackingAlert")
                        if (args.last().isEmpty()){
                            arg
                        } else {
                            val r = ArrayList<String>()
                            arg.forEach { if (it.startsWith(args.last())) r.add(it) }
                            r
                        }
                    }
                    else -> mutableListOf()
                }
            }

        }
        return mutableListOf()
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent){
        playerList.add(MyPlayer(event.player))
    }
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent){
        playerList.removeIf { it.player == event.player }
    }
}