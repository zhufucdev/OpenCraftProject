package com.zhufu.opencraft

import com.zhufu.opencraft.Base.Extend.toPrettyString
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector

object CommandExecutor : AbstractCommandExecutor {
    override fun execute(player: Player, command: List<String>): (TutorialManager.Tutorial.TutorialStep) -> Unit {
        when (command.first()) {
            "help" -> {
                player.sendMessage(
                    TextUtil.getColoredText(
                        "------------记录模式 帮助文档------------",
                        TextUtil.TextColor.GOLD,
                        true,
                        false
                    ),
                    "title <标题>           -> 设置主标题",
                    "subtitle <标题>        -> 设置副标题",
                    "position present      -> 设置位置和方向为当前值",
                    "position <x> <y> <z>  -> 设置位置为指定位置",
                    "direction present     -> 设置方向为当前方向",
                    "direction <x> <y> <z> -> 设置方向为指定方向",
                    "type <tp|linear>      -> 设置移动到当前记录点的方式<瞬移|平滑移动>(默认为前者)",
                    "delay <毫秒数>         -> 设置切换到下一步的时延(单位毫秒，1000毫秒=1秒)",
                    "done                  -> 保存并退出记录模式",
                    "exit                  -> 不保存而退出记录模式",
                    "del                   -> 删除该步骤"
                )
                return {}
            }

            "title" -> {
                if (command.size < 2) {
                    player.sendMessage(TextUtil.error("用法错误"))
                    return {}
                }
                val sb = StringBuilder()
                for (i in 1 until command.size) {
                    sb.append(command[i] + ' ')
                }
                sb.deleteCharAt(sb.lastIndex)
                val title = sb.toString()
                player.sendMessage(TextUtil.success("已将主标题设置为$title"))
                return {
                    it.title = title
                }
            }

            "subtitle" -> {
                if (command.size < 2) {
                    player.sendMessage(TextUtil.error("用法错误"))
                    return {}
                }
                val sb = StringBuilder()
                for (i in 1 until command.size) {
                    sb.append(command[i] + ' ')
                }
                sb.deleteCharAt(sb.lastIndex)
                val title = sb.toString()
                player.sendMessage(TextUtil.success("已将副标题设置为$title"))
                return {
                    it.subTitle = title
                }
            }

            "position" -> {
                if (command.size < 2) {
                    player.sendMessage(TextUtil.error("用法错误"))
                    return {}
                }
                if (command[1] == "present") {
                    player.sendMessage(TextUtil.success("已将位置和方向设置为当前值"))
                    return {
                        it.to = player.location.clone()
                    }
                } else {
                    if (command.size < 4) {
                        player.sendMessage(TextUtil.error("用法错误"))
                        return {}
                    } else {
                        val x = command[1].toDoubleOrNull()
                        val y = command[2].toDoubleOrNull()
                        val z = command[3].toDoubleOrNull()
                        if (x == null || y == null || z == null) {
                            player.sendMessage(TextUtil.error("无效坐标: ${command[1]} ${command[2]} ${command[3]}"))
                            return {}
                        }
                        val r = Location(player.world, x, y, z)
                        player.sendMessage(TextUtil.success("已将位置设置为${r.toPrettyString()}"))
                        return {
                            it.to = r
                        }
                    }
                }
            }

            "direction" -> {
                if (command.size < 2) {
                    player.sendMessage(TextUtil.error("用法错误"))
                    return {}
                }
                if (command[1] == "present") {
                    return {
                        if (it.to == null) {
                            it.to = player.location
                            player.sendMessage(TextUtil.success("已将位置和方向设置为当前值"))
                        } else {
                            it.to = it.to!!.clone().also { l ->
                                l.direction = player.location.direction
                            }
                            player.sendMessage(TextUtil.success("已将方向设置为当前方向"))
                        }
                    }
                } else {
                    if (command.size < 4) {
                        player.sendMessage(TextUtil.error("用法错误"))
                        return {}
                    } else {
                        val x = command[1].toIntOrNull()
                        val y = command[2].toIntOrNull()
                        val z = command[3].toIntOrNull()
                        if (x == null || y == null || z == null) {
                            player.sendMessage(TextUtil.error("无效坐标: ${command[1]} ${command[2]} ${command[3]}"))
                            return {}
                        }
                        val r = Vector(x, y, z)
                        player.sendMessage(TextUtil.success("已将方向设置为($x,$y,$z)"))
                        return {
                            if (it.to != null) {
                                it.to!!.direction = r
                            } else {
                                player.sendMessage(TextUtil.info("已将位置设置为当前位置"))
                                it.to = player.location
                                it.to!!.direction = r
                            }
                        }
                    }
                }
            }

            "type" -> {
                if (command.size < 2) {
                    player.sendMessage(TextUtil.error("用法错误"))
                    return {}
                }
                fun msg() {
                    player.sendMessage(TextUtil.success("已将移动方式设置为${command[1]}"))
                }
                when (command[1]) {
                    "tp" -> return {
                        msg()
                        it.type = TutorialManager.Tutorial.TutorialSwitchType.Teleport
                    }

                    "linear" -> return {
                        msg()
                        it.type = TutorialManager.Tutorial.TutorialSwitchType.Linear
                    }

                    else -> {
                        player.sendMessage(TextUtil.error("无效参数: ${command[1]}, 值只能为<tp|linear>"))
                    }
                }
            }

            "delay" -> {
                if (command.size < 2) {
                    player.sendMessage(TextUtil.error("用法错误"))
                    return {}
                }
                val num = command[1].toLongOrNull()
                if (num == null) {
                    player.sendMessage(TextUtil.error("无效参数: ${command[1]} 参数必须是自然数"))
                    return {}
                }
                player.sendMessage(TextUtil.success("已将切换时延设置为$num"))
                return {
                    it.time = num
                }
            }

            "done" -> {
                TutorialListener.instance.removeRecorder(player)
                player.sendMessage(TextUtil.success("已保存"))
                return {}
            }

            "exit" -> {
                TutorialListener.instance.removeRecorder(player)
                return {
                    if (it.project.name != "NULL") {
                        it.project[it.project.indexOf(it)] = TutorialListener.instance.originData[player]!!
                    } else {
                        it.project.removeStep(it.project.indexOf(it))
                    }
                    player.sendMessage(TextUtil.info("已退出而不保存"))
                }
            }

            "del" -> {
                TutorialListener.instance.removeRecorder(player)
                return {
                    it.project.removeStep(it.project.indexOf(it))
                    player.sendMessage(TextUtil.info("已删除该步骤"))
                }
            }
        }

        player.sendMessage(TextUtil.error("用法错误"))
        player.sendMessage(TextUtil.tip("输入\"help\"查看帮助"))
        return {}
    }
}