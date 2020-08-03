package com.zhufu.opencraft

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class BuilderCommandExecutor: TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            if (sender !is Player) {
                sender.sendMessage(TextUtil.error("只有玩家才能使用此命令"))
                return true
            }
            val info = PlayerManager.findInfoByPlayer(sender)
            if (info == null) {
                sender.error(Language.getDefault("player.error.unknown"))
                return true
            }
            if (sender.info()?.isBuilder != true) {
                sender.sendMessage(TextUtil.error("只有建筑者才能使用此命令"))
                return true
            }

            if (info.status != Info.GameStatus.MiniGaming && info.status != Info.GameStatus.InTutorial && info.status != Info.GameStatus.Observing) {
                BuilderListener.switch(sender)
            } else {
                sender.sendMessage(TextUtil.error("抱歉，但您不能在此时使用此命令"))
                return true
            }
        } else if (args.size >= 2) {
            if (!sender.isOp) {
                sender.sendMessage(TextUtil.error("您没有权限使用此命令"))
                return true
            }
            val player = Bukkit.getOfflinePlayer(args[1])
            when (args.first()) {
                "pass" -> {
                    if (player.offlineInfo()?.isBuilder != true) {
                        BuilderListener.updatePlayerLevel(player, 3)
                        sender.sendMessage(TextUtil.success("以给予${args.last()}建筑者的身份"))
                        player.player?.sendTitle("", TextUtil.info("您已被管理员给予建筑者的身份"), 7, 60, 7)
                    }
                }
                "rollback" -> {
                    if (player.offlineInfo()?.isBuilder == true) {
                        BuilderListener.updatePlayerLevel(player, 0)
                        sender.sendMessage(TextUtil.success("以夺去${args.last()}建筑者的身份"))
                        player.player?.sendTitle("", TextUtil.info("您已被管理员夺去建筑者的身份"), 7, 60, 7)
                    }
                }
                "set" -> {
                    val num = args[2].toIntOrNull()
                    if (num == null || num < 1) {
                        sender.sendMessage(TextUtil.error("无效参数: ${args[2]}: 参数不是自然数"))
                        return true
                    }
                    BuilderListener.updatePlayerLevel(player, num)
                    sender.sendMessage(TextUtil.success("已将${player.name}的建筑者等级设置为$num"))
                    player.player?.sendMessage(TextUtil.info("您的建筑者等级已更新为$num"))
                }
                else -> {
                    sender.sendMessage(TextUtil.error("用法错误"))
                }
            }
        } else {
            sender.sendMessage(TextUtil.error("用法错误"))
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
                val commands = mutableListOf("pass", "rollback", "set")
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
            } else if (args.size == 2) {
                val p = ArrayList<String>()
                fun addAllOffline() {
                    OfflineInfo.forEach {
                        p.add(it.name ?: return@forEach)
                    }
                }
                when {
                    args.first() == "pass" -> addAllOffline()
                    args.first() == "rollback" ->
                        OfflineInfo.forEach { if (it.isBuilder) p.add(it.name ?: return@forEach) }
                    args.first() == "set" -> addAllOffline()
                }

                return if (args[1].isEmpty()) {
                    p
                } else {
                    val r = ArrayList<String>()
                    p.forEach {
                        if (it.startsWith(args[1]))
                            r.add(it)
                    }
                    r
                }
            }
        }
        return mutableListOf()
    }
}