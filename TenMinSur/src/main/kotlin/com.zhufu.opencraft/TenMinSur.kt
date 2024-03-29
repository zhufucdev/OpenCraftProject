package com.zhufu.opencraft

import com.zhufu.opencraft.games.TMS
import com.zhufu.opencraft.util.toErrorMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class TenMinSur : JavaPlugin() {
    override fun onEnable() {
        GameManager.registerGameType(TMS::class)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "tms" && args.first() == "join"){
            if (sender !is Player){
                sender.sendMessage("此命令只能被玩家运行".toErrorMessage())
                return false
            }
            GameManager.joinPlayerCorrectly(sender,"TMS")
            return true
        }
        return false
    }
}