package com.zhufu.opencraft

import com.zhufu.opencraft.games.CW
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class ChunkWall : JavaPlugin() {
    override fun onEnable() {
        if (!config.getBoolean("customize", false)) {
            /**
             * Percentages of Coal by Y-Ray
             */
            val map = HashMap<String, String>()
            map["0-5"] = "0.0044 * y"
            map["6-51"] = "0.02"
            map["52-57"] = "-0.0005 * y + 0.0455"
            map["58-100"] = "0.15 / y - 0.0015"
            config.set("coal", listOf(map.toMap()))
            /**
             * Percentages of Iron by Y-Ray
             */
            map.clear()
            map["0-5"] = "0.0024 * y"
            map["6-50"] = "0.012"
            map["51-64"] = "-0.000534 * (y-50.3)^2 + 0.102"
            config.set("iron", listOf(map.toMap()))
            /**
             * Percentages of Gold by Y-Ray
             */
            map.clear()
            map["0-5"] = "0.00044 * y"
            map["6-28"] = "0.0022"
            map["29-31"] = "-0.0011 * y + 0.0341"
            config.set("gold", listOf(map.toMap()))
            /**
             * Percentages of Diamond by Y-Ray
             */
            map.clear()
            map["0-5"] = "0.0004 * y"
            map["6-12"] = "0.002 * y"
            map["13-15"] = "-0.00067 * y + 0.01"
            config.set("diamond", listOf(map.toMap()))

            saveConfig()
        }

        if (!GameManager.isInit)
            GameManager.init(this)
        CW.originWorld = WorldCreator("game_cw_origin").createWorld()!!

        GameManager.registerGameType(CW::class)
        GameManager.addNew("CW")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "cw") {
            return if (sender !is Player) {
                sender.error("此命令只能被玩家执行")
                true
            } else if (args.isEmpty() || args.first() != "join") {
                sender.error("用法错误")
                false
            } else {
                GameManager.joinPlayerCorrectly(sender, "CW")
                true
            }
        }
        return false
    }
}