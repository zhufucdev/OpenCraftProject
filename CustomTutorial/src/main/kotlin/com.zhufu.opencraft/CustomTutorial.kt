package com.zhufu.opencraft

import com.zhufu.opencraft.ui.EditorUI
import com.zhufu.opencraft.ui.TriggerUI
import com.zhufu.opencraft.ui.TutorialExp
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Logger

class CustomTutorial : JavaPlugin(), PluginBase {
    companion object {
        fun getFile(name: String) = File(saveDir, "$name.json").also { if (!it.exists()) it.createNewFile() }
        val saveDir = File("plugins${File.separatorChar}tutorials").also { if (!it.exists()) it.mkdirs() }
        lateinit var logger: Logger
    }

    override fun onEnable() {
        CustomTutorial.logger = logger

        server.pluginManager.registerEvents(TutorialListener(), this)
        TutorialManager.init(this)
        try {
            TutorialManager.loadFromFile()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ServerCaller["showTutorialUI"] = {
            val arg = it.firstOrNull()
            if (arg is Player) {
                showUI(arg)
            }
        }
    }

    private fun showUI(player: Player) {
        val pC = TutorialListener.mInstance.inCreation[player]
        val pR = TutorialListener.mInstance.inRecording[player]
        if (pC != null && pR == null) {
            EditorUI(pC).show(player)
        } else if (pC == null && pR == null) {
            val info = PlayerManager.findInfoByPlayer(player)
            if (info == null) {
                player.error(Language.getDefault("player.error.unknown"))
                return
            }
            if (info.status != Info.GameStatus.Surviving && !player.isOp) {
                player.sendMessage(TextUtil.error("您只有在生存模式下才能使用此命令"))
                return
            }

            TutorialExp(player).show()
        }
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (command.name == "ct") {
            if (sender !is Player) {
                sender.sendMessage(TextUtil.error("只有玩家才能使用此命令"))
                return true
            }

            if (args.isEmpty()) {
                showUI(sender)
            } else {
                val pC = TutorialListener.mInstance.inCreation[sender]
                val pR = TutorialListener.mInstance.inRecording[sender]
                when (args.first()) {
                    "reload" -> {
                        if (!sender.isOp) {
                            sender.error("您没有权限使用此命令")
                            return true
                        }
                        try {
                            TutorialManager.loadFromFile()
                            sender.success("完成")
                        } catch (e: Exception) {
                            sender.error("${e::class.simpleName}: ${e.message}")
                        }
                    }
                    "save" -> {
                        if (pC == null) {
                            sender.sendMessage(arrayOf(TextUtil.error("您不在编辑模式"), TextUtil.info("使用/ct 新建教程")))
                            return true
                        }
                        if (pR != null) {
                            sender.sendMessage(
                                arrayOf(
                                    TextUtil.error("您的操作已被阻止, 因为: 您仍在编辑模式"),
                                    TextUtil.tip("输入\"done\"以保存并退出")
                                )
                            )
                            return true
                        }
                        if (args.size < 2) {
                            if (pC.isDraft) {
                                sender.sendMessage(TextUtil.error("用法错误"))
                                return true
                            } else {
                                sender.sendMessage(TextUtil.success("正在保存"))
                            }
                        } else {
                            pC.isDraft = false
                            pC.name = args[1]
                            sender.sendMessage(TextUtil.success("正在将教程保存为${args[1]}"))
                        }
                        try {
                            TutorialManager.saveToFile(pC)
                        } catch (e: Exception) {
                            logger.warning("Unable to save tutorial as ${pC.name}.")
                            e.printStackTrace()
                            sender.sendMessage(TextUtil.printException(e))
                            sender.sendMessage(TextUtil.error("失败"))
                            return true
                        }
                        TutorialListener.mInstance.removeCreator(sender)
                        sender.sendMessage(TextUtil.success("完成"))
                    }
                    "saveas" -> {
                        if (pC == null) {
                            sender.sendMessage(arrayOf(TextUtil.error("您不在编辑模式"), TextUtil.info("使用/ct 新建教程")))
                            return true
                        }
                        if (pR != null) {
                            sender.sendMessage(
                                arrayOf(
                                    TextUtil.tip("您的操作已被阻止, 因为: 您仍在编辑模式"),
                                    TextUtil.tip("输入\"done\"以保存并退出")
                                )
                            )
                            return true
                        }
                        if (args.size < 2) {
                            sender.sendMessage(TextUtil.error("用法错误"))
                            return true
                        }
                        pC.isDraft = false
                        pC.name = args[1]
                        pC.id = TutorialManager.getNewID()
                        TutorialManager.add(TutorialListener.mInstance.originProjectData[sender]!!)

                        TutorialListener.mInstance.removeCreator(sender)
                        sender.sendMessage(TextUtil.success("已将教程另存为${args[1]}"))
                    }
                    "exit" -> {
                        if (pC == null) {
                            sender.sendMessage(arrayOf(TextUtil.error("您不在编辑模式"), TextUtil.info("使用/ct 新建教程")))
                            return true
                        }
                        if (pR != null) {
                            sender.sendMessage(
                                arrayOf(
                                    TextUtil.tip("您的操作已被阻止, 因为: 您仍在编辑模式"),
                                    TextUtil.tip("输入\"done\"以保存并退出")
                                )
                            )
                            return true
                        }
                        if (!pC.isDraft) {
                            TutorialManager[pC.id] = TutorialListener.mInstance.originProjectData[sender]!!
                                .also { it.isDraft = false }
                            sender.sendMessage(TextUtil.info("不保存并退出"))
                        } else {
                            sender.sendMessage(TextUtil.info("教程已保存为草稿"))
                        }
                        TutorialListener.mInstance.removeCreator(sender)
                    }

                    "trigger" -> {
                        if (pC == null) {
                            sender.sendMessage(arrayOf(TextUtil.error("您不在编辑模式"), TextUtil.info("使用/ct 新建教程")))
                            return true
                        }
                        when (args[1]) {
                            "none" -> {
                                pC.triggerMethod = TutorialManager.Tutorial.TriggerMethod.NONE
                            }
                            "block" -> {
                                fun msg() {
                                    sender.sendMessage(
                                        arrayOf(
                                            TextUtil.error("用法错误"),
                                            TextUtil.info(TriggerUI.helpDoc[1])
                                        )
                                    )
                                }
                                if (args.size < 5) {
                                    msg()
                                    return true
                                }
                                pC.triggerMethod = TutorialManager.Tutorial.TriggerMethod.BLOCK
                                pC.rebuildArguments()

                                val x = args[2].toIntOrNull()
                                val y = args[3].toIntOrNull()
                                val z = args[4].toIntOrNull()
                                if (x == null || y == null || z == null) {
                                    msg()
                                    return true
                                }

                                pC.triggerArgument
                                    .also {
                                        it.addProperty("x", x)
                                        it.addProperty("y", y)
                                        it.addProperty("z", z)
                                        it.addProperty("world", sender.world.uid.toString())
                                    }
                            }
                            "territory" -> {
                                fun msg(t: String) {
                                    sender.sendMessage(arrayOf(TextUtil.error(t), TriggerUI.helpDoc[2]))
                                }
                                if (args.size < 3) {
                                    msg("用法错误")
                                    return true
                                }
                                val info = BlockLockManager[args[2]]
                                if (info == null) {
                                    msg("区块不存在")
                                    return true
                                }
                                if (info.canAccess(sender)) {
                                    pC.rebuildArguments()
                                    pC.triggerMethod = TutorialManager.Tutorial.TriggerMethod.TERRITORY
                                    pC.triggerArgument.addProperty("name", args[2])
                                } else {
                                    msg("您没有权限访问此区块")
                                    return true
                                }
                            }
                            "enter-world" -> {
                                if (!sender.isOp) {
                                    sender.sendMessage(TextUtil.error("您没有权限使用此方式"))
                                    return true
                                }
                                if (args.size < 3) {
                                    sender.sendMessage(arrayOf(TextUtil.error("用法错误"), TriggerUI.helpDoc[3]))
                                    return true
                                }
                                val world = Bukkit.getWorld(args[2])
                                if (world == null) {
                                    sender.sendMessage(TextUtil.error("世界不存在"))
                                    return true
                                }
                                pC.rebuildArguments()
                                pC.triggerMethod = TutorialManager.Tutorial.TriggerMethod.ENTER_WORLD
                                pC.triggerArgument.addProperty("world", world.uid.toString())
                            }
                            "register" -> {
                                if (!sender.isOp) {
                                    sender.sendMessage(TextUtil.error("您没有权限使用此方式"))
                                    return true
                                }
                                pC.rebuildArguments()
                                pC.triggerMethod = TutorialManager.Tutorial.TriggerMethod.REGISTER
                            }
                        }
                        sender.sendMessage(TextUtil.success("已将触发方式设置为${args[1]} ${pC.triggerArgument}"))
                    }

                    else -> {
                        sender.sendMessage(TextUtil.error("用法错误"))
                    }
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
        if (command.name == "ct") {
            if (sender !is Player)
                return mutableListOf()

            TutorialListener.mInstance.inCreation[sender] ?: return mutableListOf()

            if (args.size == 1) {
                val commands = mutableListOf("save", "saveas", "exit", "trigger","reload")
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
            } else if (args.size == 2 && args.first() == "trigger") {
                val commands = mutableListOf("none", "block", "territory", "enter-world", "register")
                return if (args[1].isEmpty()) {
                    commands
                } else {
                    val r = ArrayList<String>()
                    commands.forEach {
                        if (it.startsWith(args[1]))
                            r.add(it)
                    }
                    r
                }
            } else if (args.size >= 3 && args.first() == "trigger") {
                when (args[1]) {
                    "none" -> sender.sendMessage(TriggerUI.helpDoc.first())
                    "block" -> {
                        val block = sender.getTargetBlock(null, 5)
                        return when (args.size) {
                            3 -> mutableListOf(block.x.toString())
                            4 -> mutableListOf(block.y.toString())
                            5 -> mutableListOf(block.z.toString())
                            else -> mutableListOf()
                        }
                    }
                    "territory" -> {
                        if (args.size == 3) {
                            val commands = ArrayList<String>()
                            BlockLockManager.forEach {
                                if (it.canAccess(sender))
                                    commands.add(it.name)
                            }
                            return if (args[2].isEmpty()) {
                                commands
                            } else {
                                val r = ArrayList<String>()
                                commands.forEach { if (it.startsWith(args[2])) r.add(it) }
                                return r
                            }
                        }
                    }
                    "enter-world" -> if (sender.isOp && args.size == 3) {
                        val worlds = ArrayList<String>()
                        Bukkit.getWorlds().forEach {
                            worlds.add(it.name)
                        }
                        return if (args[2].isEmpty()) {
                            worlds
                        } else {
                            val r = ArrayList<String>()
                            worlds.forEach { if (it.startsWith(args[2])) r.add(it) }
                            return r
                        }
                    }
                }
            }
        }
        return mutableListOf()
    }
}