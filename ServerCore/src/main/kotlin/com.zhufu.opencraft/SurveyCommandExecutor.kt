package com.zhufu.opencraft

import com.zhufu.opencraft.survey.SurveyManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class SurveyCommandExecutor : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty()) {
            if (!sender.isOp) {
                sender.sendMessage(TextUtil.error("您没有权限使用该命令"))
                return true
            }
            if (args.size < 2) {
                sender.sendMessage(TextUtil.error("用法错误"))
                return true
            }
            when (args.first()) {
                "pass" -> {
                    val it = PlayerManager.findOfflineInfoByPlayer(Bukkit.getOfflinePlayer(args[1]).uniqueId)
                    if (it == null) {
                        sender.sendMessage(TextUtil.error("找不到玩家"))
                        return true
                    }
                    if (it.isOnline) {
                        it.onlinePlayerInfo!!.player.sendTitle("", TextUtil.info("您已被管理员给予正式会员的身份"), 7, 60, 7)
                    }
                    it.isSurveyPassed = true
                }
                "rollback" -> {
                    val it = PlayerManager.findOfflineInfoByPlayer(Bukkit.getOfflinePlayer(args[1]).uniqueId)
                    if (it == null) {
                        sender.sendMessage(TextUtil.error("找不到玩家"))
                        return true
                    }
                    if (it.isOnline) {
                        it.onlinePlayerInfo!!.player.sendTitle("", TextUtil.info("您已被管理员剥夺正式会员的身份"), 7, 60, 7)
                    }
                    it.isSurveyPassed = false
                }
                "giveChance" -> {
                    val it = PlayerManager.findOfflineInfoByPlayer(Bukkit.getOfflinePlayer(args[1]).uniqueId)
                    if (it == null) {
                        sender.sendMessage(TextUtil.error("找不到玩家"))
                        return true
                    }
                    val num = args[2].toIntOrNull()
                    if (num == null) {
                        sender.sendMessage(TextUtil.error("非法参数 ${args[2]}: 参数不是整数"))
                        return true
                    }
                    it.remainingSurveyChance += num
                    if (it.isOnline) {
                        it.onlinePlayerInfo!!.player.sendMessage(TextUtil.info("您被给予${num}次参与服务器调查的机会，您现在有${it.remainingSurveyChance}次机会"))
                    }
                }
            }
        } else {
            if (sender !is Player) {
                sender.sendMessage(TextUtil.error("只有玩家才能使用此命令"))
                return true
            }
            SurveyManager.startSurvey(sender)
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (sender.isOp) {
            if (args.size == 1) {
                val commands = mutableListOf("pass", "rollback", "giveChance")
                return if (args.first().isEmpty()) {
                    commands
                } else {
                    val r = ArrayList<String>()
                    commands.forEach { if (it.startsWith(args.first())) r.add(it) }
                    r
                }
            } else if (args.size == 2) {
                val p = ArrayList<String?>()
                OfflineInfo.forEach {
                    if (args.first() == "pass" && !it.isSurveyPassed)
                        p.add(Bukkit.getOfflinePlayer(it.uuid!!).name)
                    else if (args.first() == "rollback" && it.isSurveyPassed)
                        p.add(Bukkit.getOfflinePlayer(it.uuid!!).name)
                    else if (args.first() == "giveChance")
                        p.add(Bukkit.getOfflinePlayer(it.uuid!!).name)
                }
                return if (args[1].isEmpty()) {
                    p.filterNotNull().toMutableList()
                } else {
                    val r = ArrayList<String>()
                    p.forEach { if (it != null && it.startsWith(args[1])) r.add(it) }
                    r
                }
            }
        }
        return mutableListOf()
    }
}