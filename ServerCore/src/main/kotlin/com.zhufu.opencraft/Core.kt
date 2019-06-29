package com.zhufu.opencraft

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import com.gmail.filoghost.holographicdisplays.api.Hologram
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI
import com.zhufu.opencraft.Base.spawnWorld
import com.zhufu.opencraft.Base.lobby
import com.zhufu.opencraft.Base.surviveWorld
import com.zhufu.opencraft.Base.Extend.isDigit
import com.zhufu.opencraft.Game.env
import com.zhufu.opencraft.PlayerManager.showPlayerOutOfDemoTitle
import com.zhufu.opencraft.Game.varNames
import com.zhufu.opencraft.PlayerManager.containsSpecialItem
import com.zhufu.opencraft.PlayerManager.specialItems
import com.zhufu.opencraft.Base.endWorld
import com.zhufu.opencraft.Base.netherWorld
import com.zhufu.opencraft.chunkgenerator.VoidGenerator
import com.zhufu.opencraft.listener.*
import com.zhufu.opencraft.survey.SurveyManager
import com.zhufu.opencraft.events.PlayerInventorySaveEvent
import com.zhufu.opencraft.events.PlayerJoinGameEvent
import com.zhufu.opencraft.events.PlayerRegisterEvent
import com.zhufu.opencraft.headers.ServerHeaders
import com.zhufu.opencraft.headers.server_wrap.SimpleServerListPingEvent
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.player_community.PlayerStatics
import com.zhufu.opencraft.script.AbstractScript
import com.zhufu.opencraft.script.ServerScript
import com.zhufu.opencraft.special_items.FlyWand
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.SpawnReason
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Equipment
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.util.Vector
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.roundToLong

class Core : JavaPlugin(), Listener {
    companion object {
        var npc = ArrayList<NPC>()
    }

    private var reloadTask: BukkitTask? = null
    private var saveTask: BukkitTask? = null
    private lateinit var scoreBoardTask: BukkitTask
    override fun onEnable() {
        //World initiations
        spawnWorld = server.createWorld(
            WorldCreator.name("world_spawn")
                .type(WorldType.CUSTOMIZED)
                .generator(VoidGenerator(2))
        )!!
        spawnWorld.peace()
        surviveWorld = server.createWorld(WorldCreator("world_survive"))!!
        netherWorld = server.getWorld("world_nether")!!
        endWorld = server.getWorld("world_the_end")!!
        lobby = server.getWorld("world")!!
        lobby.peace()

        env =
            try {
                YamlConfiguration.loadConfiguration(File(dataFolder, "env").also {
                    if (!it.parentFile.exists())
                        it.parentFile.mkdirs()
                    if (!it.exists()) it.createNewFile()
                })
            } catch (e: Exception) {
                YamlConfiguration()
            }
        handleTasks()

        with(Bukkit.getPluginManager()) {
            val self = this@Core
            registerEvents(self, self)
            registerEvents(NPCListener, self)
            registerEvents(NPCSelectListener(npc.toTypedArray()), self)
            registerEvents(SurviveListener(self), self)
        }
        if (!dataFolder.exists()) dataFolder.mkdirs()
        try {
            ServerStatics.init()
            SurveyManager.init(File(dataFolder, "survey.json"), this)
            GameManager.init(this)
            PlayerManager.init(this)
            TradeManager.init(this)
            BuilderListener.init(this)
            PlayerObserverListener.init(this)
            AbstractScript.threadPool = Executors.newCachedThreadPool()
            PlayerLobbyManager.init()
            Scripting
            try {
                ServerScript.INSTANCE.call()
                ServerHeaders.serverSelf.onServerBoot.forEach {
                    it.apply(null)
                }
            } catch (e: Exception) {
                logger.warning("Failed to execute AutoExec script.")
                e.printStackTrace()
            }
        } catch (e: Throwable) {
            logger.warning("Error while initializing Server Core.")
            e.printStackTrace()
        }

        ServerCaller["SolvePlayerLobby"] = {
            val info = (it.firstOrNull()
                ?: throw IllegalArgumentException("This call must be give at least one Info parameter.")) as Info
            PlayerLobbyManager[info].also { lobby -> if (!lobby.isInitialized) lobby.initialize() }.tpThere(info.player)
        }

        if (!server.pluginManager.isPluginEnabled("HolographicDisplays")) {
            logger.warning("HolographicDisplays is not enabled.")
            logger.warning("Disabling floating text functionality.")
        } else {
            spawnHolographicText()
        }
        if (!server.pluginManager.isPluginEnabled("Citizens")) {
            logger.warning("Citizens is not enabled.")
            logger.warning("Disabling NPC functionality.")
        } else {
            Bukkit.getScheduler().runTaskLater(this, { _ ->
                spawnNPC()
            }, 40)
        }
    }

    override fun onDisable() {
        npc.forEach {
            it.despawn()
            it.destroy()
        }
        PlayerObserverListener.onServerStop()
        PlayerManager.forEachPlayer { it.saveServerID() }
        BuilderListener.onServerClose()
        PlayerStatics.cleanUp()
        Base.publicMsgPool.serialize().save(Base.msgPoolFile)
        PlayerLobbyManager.onServerClose()
        Scripting.cleanUp()
        ServerStatics.save()
    }

    private var urlLooper = 0
    private var looperDirection = true
    private fun handleTasks() {
        if (!env.isSet("reloadDelay"))
            env.set("reloadDelay", 2 * 60 * 20)
        if (!env.isSet("inventorySaveDelay"))
            env.set("inventorySaveDelay", 4 * 60 * 20)
        if (!env.isSet("prisePerBlock"))
            env.set("prisePerBlock", 10)
        if (!env.isSet("backToDeathPrise"))
            env.set("backToDeathPrise", 3)
        if (!env.isSet("countOfSurveyQuestion"))
            env.set("countOfSurveyQuestion", 6)
        if (!env.isSet("secondsPerQuestion"))
            env.set("secondsPerQuestion", 30)
        if (!env.isSet("url"))
            env.set("url", "https://www.open-craft.cn")
        env.save(File(dataFolder, "env"))
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            Bukkit.getPluginManager().callEvent(PlayerInventorySaveEvent())
            PlayerManager.forEachPlayer {
                if (it.status == Info.GameStatus.Surviving) {
                    val successful = try {
                        it.saveServerID()
                        true
                    } catch (e: Exception) {
                        logger.warning("Error while saving inventory for player ${it.player.name}")
                        e.printStackTrace()
                        it.player.sendMessage(TextUtil.printException(e))
                        false
                    }
                    if (successful)
                        it.player.info(Language[it, "server.saveInventory"])
                }
            }
        }, 0, env.getInt("inventorySaveDelay").toLong())

        scoreBoardTask = Bukkit.getScheduler().runTaskTimer(this, Runnable {
            PlayerManager.forEachPlayer { info ->
                val getter = getLangGetter(info)

                info.statics!!.timeToday += 2

                if (info.status != Info.GameStatus.MiniGaming) {
                    var sub = 0

                    val newBoard = Bukkit.getScoreboardManager().newScoreboard
                    val obj = newBoard.registerNewObjective("serverStatics", "dummy", getter["server.statics.title"])
                    obj.displaySlot = DisplaySlot.SIDEBAR
                    obj.getScore(TextUtil.info(getter["server.statics.coinCount", info.currency])).score = --sub
                    if (!info.isSurveyPassed) {
                        obj.getScore(TextUtil.info(getter["server.statics.demoTime", info.remainingDemoTime / 1000L / 60]))
                            .score = --sub
                        if (info.remainingDemoTime <= 0 && info.status != Info.GameStatus.InLobby) {
                            PlayerManager.onPlayerOutOfDemo(info)
                        }
                    }
                    if (info.status != Info.GameStatus.InLobby && info.status != Info.GameStatus.InTutorial) {
                        info.gameTime += 2 * 1000L
                        ServerStatics.onlineTime += 2 * 1000L
                    }

                    if (BuilderListener.isInBuilderMode(info.player)) {
                        obj.getScore(
                            TextUtil.getColoredText(
                                getter["server.statics.builderLevel", info.builderLevel],
                                TextUtil.TextColor.BLUE,
                                false,
                                underlined = false
                            )
                        ).score = --sub
                    }

                    var allowFlight =
                        info.player.gameMode == GameMode.CREATIVE || info.player.gameMode == GameMode.SPECTATOR
                    if (info.player.inventory.containsSpecialItem) {
                        info.player.inventory.specialItems.forEach { item ->
                            if (item is FlyWand) {
                                if (!allowFlight) {
                                    if (info.player.isFlying) {
                                        item.updateTime(item.timeRemaining - 2)
                                        if (item.inventoryPosition != -1)
                                            info.player.inventory.setItem(item.inventoryPosition, item)
                                    }
                                    if (!item.isUpToTime) {
                                        allowFlight = true
                                    }
                                    obj.getScore(
                                        TextUtil.getColoredText(
                                            getter["server.statics.flyRemaining", item.timeRemaining],
                                            TextUtil.TextColor.RED
                                        )
                                    ).score = --sub
                                }
                            }
                        }
                    }
                    info.player.allowFlight = allowFlight

                    val url = env.getString("url")
                    obj.getScore(
                        buildString {
                            val all = TextUtil.TextColor.AQUA.getCode()
                            val special = TextUtil.TextColor.WHITE.getCode()
                            append(url)
                            insert(urlLooper, special)
                            insert(urlLooper + 3, all)
                            insert(0, all)
                        }
                    ).score = --sub
                    if (urlLooper >= url!!.lastIndex || (urlLooper <= 0 && !looperDirection)) {
                        looperDirection = !looperDirection
                    }
                    urlLooper += if (looperDirection) 1 else -1
                    info.player.scoreboard = newBoard
                }
            }
        }, 0L, 2 * 20)
    }

    private var title: Hologram? = null
    private var content: Hologram? = null
    private fun spawnHolographicText() {
        val location = env.getSerializable("notice", Location::class.java, null) ?: return
        if (title == null)
            title = HologramsAPI.createHologram(this, location.clone().add(Vector(0, 1, 0)))
        title!!.clearLines()
        title!!.appendTextLine(TextUtil.getColoredText("OpenCraft公告栏", TextUtil.TextColor.AQUA, true, false))

        if (content == null)
            content = HologramsAPI.createHologram(this, location)
        content!!.clearLines()

        var i = 1
        val noticeFile = File(dataFolder.also { if (!it.exists()) it.mkdirs() }, "notice.txt")
        if (!noticeFile.exists()) noticeFile.createNewFile()
        val reader = noticeFile.bufferedReader()
        reader.forEachLine {
            content!!.appendTextLine(TextUtil.getCustomizedText(it))
            i++
        }
    }

    private fun spawnNPC() {
        val data = File(dataFolder, "npc")
        if (!data.exists()) data.mkdirs()
        npc.forEach {
            it.despawn()
            it.destroy()
        }
        npc.clear()
        data.listFiles()?.forEach {
            if (!it.isHidden) {
                try {
                    npc.add(readNPCProfile(it))
                } catch (e: Exception) {
                    throw IllegalStateException("Error while loading ${it.name}", e.cause)
                }
            }
        }
    }

    private fun readNPCProfile(file: File): NPC {
        logger.info("Loading NPC file ${file.name}")

        try {
            val conf = YamlConfiguration.loadConfiguration(file)
            val registry = CitizensAPI.getNPCRegistry()

            val name = TextUtil.getCustomizedText(conf.getString("name", "Unknown")!!)
            val type = EntityType.valueOf(conf.getString("type", "PLAYER")!!.toUpperCase())
            val world = Bukkit.getWorld(conf.getString("world", "world")!!)
            val x = conf.getString("location.x")!!.toDouble()
            val y = conf.getString("location.y")!!.toDouble()
            val z = conf.getString("location.z")!!.toDouble()
            val faceX = conf.getString("face.x")!!.toDouble()
            val faceY = conf.getString("face.y")!!.toDouble()
            val faceZ = conf.getString("face.z")!!.toDouble()
            val rightHand =
                if (conf.getString("items.rightHand") != null) Material.valueOf(conf.getString("items.rightHand")!!) else null
            val leftHand =
                if (conf.getString("items.leftHand") != null) Material.valueOf(conf.getString("items.leftHand")!!) else null
            val chest =
                if (conf.getString("items.chest") != null) Material.valueOf(conf.getString("items.chest")!!) else null
            val leggings =
                if (conf.getString("items.leggings") != null) Material.valueOf(conf.getString("items.leggings")!!) else null
            val boots =
                if (conf.getString("items.boots") != null) Material.valueOf(conf.getString("items.boots")!!) else null
            val helmet =
                if (conf.getString("items.helmet") != null) Material.valueOf(conf.getString("items.helmet")!!) else null
            val click = conf.getString("click", "none")
            val near = conf.getString("near", "none")

            val npc = registry.createNPC(type, name)
            npc.data().set("click", click)
            npc.data().set("near", near)
            npc.spawn(
                Location(world, x, y, z)
                    .setDirection(Vector(faceX, faceY, faceZ)),
                SpawnReason.PLUGIN
            )
            val trait = npc.getTrait(Equipment::class.java)

            Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
                try {
                    if (type == EntityType.PLAYER) {
                        val player = npc.entity as Player
                        with(trait) {
                            if (helmet != null) set(Equipment.EquipmentSlot.HELMET, ItemStack(helmet))
                            if (chest != null) set(Equipment.EquipmentSlot.CHESTPLATE, ItemStack(chest))
                            if (leggings != null) set(Equipment.EquipmentSlot.LEGGINGS, ItemStack(leggings))
                            if (boots != null) set(Equipment.EquipmentSlot.BOOTS, ItemStack(boots))
                        }
                        with(player.inventory) {
                            if (leftHand != null) setItemInMainHand(ItemStack(leftHand))
                            if (rightHand != null) setItemInOffHand(ItemStack(rightHand))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return npc
        } catch (e: Exception) {
            throw IllegalStateException("Unable to load ${file.nameWithoutExtension}: ${e.message}", e.cause)
        }
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (command.name == "server") {
            when {
                args.isEmpty() -> {
                    sender.sendMessage("用法错误")
                    return false
                }
                args.first() == "feedback" -> {
                    if (args.size < 2) {
                        sender.sendMessage(TextUtil.error("您没有输入文本"))
                        return false
                    }
                    val sb = StringBuilder()
                    for (i in 1 until args.size)
                        sb.append("${args[i]} ")
                    sb.deleteCharAt(sb.length - 1)

                    if (sb.length > 300) {
                        sender.sendMessage(TextUtil.error("字数超过上限(300字)"))
                        return true
                    }

                    val file = File(
                        "$dataFolder${File.separatorChar}feedback${File.separatorChar}${SimpleDateFormat("yyyy${File.separatorChar}MM${File.separatorChar}dd${File.separatorChar}HH:mm").format(
                            Date()
                        )}-${sender.name}.txt"
                    )
                    file.parentFile.mkdirs()
                    file.createNewFile()
                    file.writeText(sb.toString())

                    PlayerManager.forEachOffline {
                        if (it.offlinePlayer.isOp) {
                            it.messagePool.add(
                                TextUtil.info("收到来自${sender.name} 的反馈:") + sb.toString(),
                                MessagePool.Type.Friend
                            )
                        }
                    }

                    sender.sendMessage(TextUtil.info("您的反馈已提交，${TextUtil.error("感谢支持")}"))
                }
                args.first() == "about" -> {
                    val readme = File(dataFolder, "readme.txt")
                    if (!readme.exists()) {
                        sender.sendMessage(TextUtil.info("未找到自述文件"))
                        return true
                    }
                    val charset = if (args.size >= 2) try {
                        Charset.forName(args[1])
                    } catch (e: Exception) {
                        sender.sendMessage(TextUtil.error("未找到编码格式: ${args[1]}"))
                        return false
                    } else Charsets.UTF_8
                    Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
                        val path = Paths.get(
                            System.getenv("PATH").split(':').firstOrNull {
                                Paths.get(it, "screenfetch").toFile().exists()
                            }?:"null",
                            "screenfetch"
                        ).toFile()
                        val screenfetch = if (path.exists()) {
                            Runtime.getRuntime().exec(path.absolutePath)
                                .inputStream.bufferedReader()
                                .readText().filter { !it.isISOControl() || it == '\n' }
                                .replace(Regex("\\[[0-9]*;"), "[")
                                .replace("[0m", "")
                                .replace("[1m", ChatColor.RESET.toString())
                                .replace("[30m", "${ChatColor.COLOR_CHAR}${ChatColor.BLACK.char}")
                                .replace("[31m", "${ChatColor.COLOR_CHAR}${ChatColor.RED.char}")
                                .replace("[32m", "${ChatColor.COLOR_CHAR}${ChatColor.GREEN.char}")
                                .replace("[33m", "${ChatColor.COLOR_CHAR}${ChatColor.YELLOW.char}")
                                .replace("[34m", "${ChatColor.COLOR_CHAR}${ChatColor.BLUE.char}")
                                .replace("[35m", "${ChatColor.COLOR_CHAR}${ChatColor.LIGHT_PURPLE.char}")
                                .replace("[36m", "${ChatColor.COLOR_CHAR}${ChatColor.DARK_AQUA.char}")
                                .replace("[37m","${ChatColor.COLOR_CHAR}${ChatColor.WHITE}")
                                .plus(ChatColor.RESET.char)
                        } else "ERROR: no shell command screenfetch."
                        sender.sendMessage(
                            TextUtil.format(
                                title = "自述文件",
                                content = screenfetch + readme.readText(charset)
                            )
                        )
                    })
                }
                args.first() == "stop" -> {
                    if (sender !is ConsoleCommandSender && !sender.isOp) {
                        sender.sendMessage(TextUtil.error("您没有权限使用此命令"))
                        return true
                    }
                    if (args.size < 2) {
                        sender.sendMessage(TextUtil.error("请给出原因"))
                        return false
                    }
                    server.onlinePlayers.forEach {
                        it.sendTitle(TextUtil.error("服务器即将关闭"), TextUtil.info(args[1]), 7, 1000, 7)
                    }
                    Bukkit.getScheduler().runTaskLater(this, Runnable {
                        server.shutdown()
                    }, (args[1].length * 0.2 * 20).roundToLong())
                }
                args.first() == "reload" -> {
                    if (sender !is ConsoleCommandSender && !sender.isOp) {
                        sender.sendMessage(TextUtil.error("您没有权限使用此命令"))
                        return true
                    }
                    if (args.size < 2) {
                        sender.sendMessage(
                            arrayOf(
                                TextUtil.error("用法错误"),
                                server.getPluginCommand("server reload")!!.usage
                            )
                        )
                        return true
                    }

                    val failure = ArrayList<String>()
                    (1 until args.size).forEach {
                        var result = false
                        when (args[it]) {
                            "notice" -> {
                                sender.sendMessage(TextUtil.info("正在重载公告栏"))
                                spawnHolographicText()
                                result = true
                            }
                            "game" -> {
                                sender.sendMessage(TextUtil.info("正在重载游戏"))
                                result = true
                                GameManager.forEach { game ->
                                    val id = game.gameID
                                    try {
                                        game.initGameEnder()
                                    } catch (e: Exception) {
                                        result = false
                                        sender.sendMessage(TextUtil.error("gameID: $id ${e.javaClass.simpleName}: ${e.message}"))
                                        e.printStackTrace()
                                    }
                                }
                            }
                            "npc" -> {
                                sender.sendMessage(TextUtil.info("正在重载NPC"))
                                result = try {
                                    spawnNPC()
                                    true
                                } catch (e: Exception) {
                                    sender.sendMessage(TextUtil.error("${e.javaClass.simpleName}: ${e.message}"))
                                    e.printStackTrace()
                                    false
                                }
                            }
                            "survey" -> {
                                sender.sendMessage(TextUtil.info("正在重载服务器调查"))
                                SurveyManager.init(File(dataFolder, "survey.json"), null)
                                result = true
                            }
                            "lang" -> {
                                sender.info("正在重载语言文件")
                                result = try {
                                    Language.init()
                                    true
                                } catch (e: Exception) {
                                    sender.sendMessage(TextUtil.error("${e.javaClass.simpleName}: ${e.message}"))
                                    e.printStackTrace()
                                    false
                                }
                            }
                            "ss" -> {
                                sender.info("正在重载脚本")
                                result = try {
                                    ServerScript.reload()
                                    ServerScript.INSTANCE.call()
                                    true
                                } catch (e: Exception) {
                                    sender.error("${e.javaClass.simpleName}: ${e.message}")
                                    e.printStackTrace()
                                    false
                                }
                            }
                            else -> {
                                sender.sendMessage(TextUtil.error("无效模块: ${args[1]}"))
                            }
                        }
                        if (!result)
                            failure.add(args[it])
                    }
                    sender.sendMessage(
                        if (failure.isEmpty())
                            TextUtil.info("成功重载指定模块")
                        else {
                            buildString {
                                append("${ChatColor.RED}无法重载")
                                failure.forEach { append("$it,") }
                                deleteCharAt(lastIndex)
                            }
                        }
                    )
                }
                args.first() == "set" -> {
                    if (!sender.isOp) {
                        sender.sendMessage(TextUtil.error("您没有权限使用此命令"))
                        return true
                    }

                    if (args.size == 2 && args[1] == "notice") {
                        if (sender !is Player) {
                            sender.sendMessage(TextUtil.error("只有玩家才能使用此命令"))
                            return true
                        }
                        env.set("notice", sender.location)
                        env.save(File(dataFolder, "env"))
                        sender.sendMessage(TextUtil.success("已将公告栏位置设置为当前位置"))
                        spawnHolographicText()
                    } else {
                        if (args.size < 3) {
                            sender.sendMessage(TextUtil.error("用法错误"))
                            return true
                        }
                        if (args[1] == "url") {
                            env.set("url", args[2])
                            env.save(File(dataFolder, "env"))
                            sender.success("已将服务器网站地址标记为${args[2]}")
                            urlLooper = 0
                            looperDirection = true
                        } else {
                            fun checkIntOverZero(): Boolean {
                                if (!args[2].isDigit()) {
                                    sender.sendMessage(TextUtil.error("此变量只允许数字"))
                                    return false
                                }
                                if (args[2].contains('.') || args[2].contains('-')) {
                                    sender.sendMessage(TextUtil.error("此变量只允许正整数"))
                                    return false
                                }
                                return true
                            }
                            if (!varNames.contains(args[1])) {
                                sender.sendMessage(TextUtil.error("变量不存在"))
                                return true
                            }
                            if (checkIntOverZero()) {
                                env.set(args[1], args[2].toIntOrNull())
                                sender.sendMessage(TextUtil.success("已将服务器环境变量${args[1]}设置为${args[2]}"))
                                env.save(File(dataFolder, "env"))

                                reloadTask?.cancel()
                                saveTask?.cancel()
                                handleTasks()
                            }
                        }
                    }
                }
                args.first() == "notice" -> {
                    if (args.size <= 1 && sender is Player && sender.isOp) {
                        Base.publicMsgPool.sendAllTo(sender.info() ?: return true)
                        return true
                    }
                    if (args.size < 3) {
                        sender.error("用法错误")
                        return true
                    }
                    when (args[1]) {
                        "append" -> {
                            val text = buildString {
                                for (i in 2 until args.size)
                                    append(args[i] + ' ')
                                deleteCharAt(lastIndex)
                            }
                            Base.publicMsgPool.add(
                                text = text,
                                type = MessagePool.Type.Public,
                                extra = YamlConfiguration()
                            )
                            sender.success("已在公共消息池中追加该内容")
                            PlayerManager.forEachChatter { Base.publicMsgPool.sendUnreadTo(it) }
                        }
                        "remove" -> {
                            val id = args[2].toIntOrNull()
                            if (id == null || id < 0) {
                                sender.error("参数必须是自然数")
                                return true
                            }
                            Base.publicMsgPool.remove(id)
                            sender.success("已移除索引为${id}的消息")
                        }
                        "broadcast" -> PlayerManager.forEachChatter { Base.publicMsgPool.sendUnreadTo(it) }
                    }
                }
                args.first() == "script" -> {
                    val getter = sender.lang()
                    if (!sender.isOp) {
                        sender.error(getter["command.error.permission"])
                        return true
                    }
                    if (args.size < 2 || args[1].isEmpty()) {
                        sender.error(getter["command.error.usage"])
                        return true
                    }
                    val src = buildString {
                        for (i in 1 until args.size) {
                            append(args[i] + ' ')
                        }
                        if (isNotEmpty()) {
                            deleteCharAt(lastIndex)
                        }
                    }
                    Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
                        val timeBegin = System.currentTimeMillis()
                        val result = ServerScript.INSTANCE.runLine(
                            src,
                            if (sender is Player) sender.info()?.playerOutputStream else null
                        )
                        val timeEnd = System.currentTimeMillis()
                        if (result == null) {
                            sender.error(getter["scripting.returnNull", (timeEnd - timeBegin) / 1000.0])
                        } else {
                            sender.success(getter["scripting.returnSomething", (timeEnd - timeBegin) / 1000.0, result.toString()])
                        }
                    }
                }
            }
        } else if (command.name == "survey") {
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
                        val it = PlayerManager.findOfflinePlayer(Bukkit.getOfflinePlayer(args[1]).uniqueId)
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
                        val it = PlayerManager.findOfflinePlayer(Bukkit.getOfflinePlayer(args[1]).uniqueId)
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
                        val it = PlayerManager.findOfflinePlayer(Bukkit.getOfflinePlayer(args[1]).uniqueId)
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
        } else if (command.name == "builder") {
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
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (command.name == "server") {
            val first = args.first()

            if (args.size == 1) {
                val commands = mutableListOf("feedback", "about")
                if (sender.isOp) commands.addAll(listOf("stop", "reload", "set", "notice", "script"))
                return if (first.isEmpty()) {
                    commands
                } else {
                    commands.filter { it.startsWith(first) }.toMutableList()
                }
            } else {
                when (first) {
                    "about" -> {
                        val sets = listOf(
                            Charsets.UTF_8.name(),
                            Charsets.UTF_16.name(),
                            Charsets.US_ASCII.name(),
                            Charsets.ISO_8859_1.name()
                        )
                        if (args.size == 1)
                            return sets.toMutableList()
                        else if (args.size == 2) {
                            val result = ArrayList<String>()
                            sets.forEach {
                                if (it.startsWith(args[1])) result.add(it)
                            }
                            return result.toMutableList()
                        }
                    }
                    "reload" -> {
                        if (!sender.isOp) {
                            return mutableListOf()
                        }
                        val models = listOf("game", "notice", "npc", "survey")
                        if (args.size == 1)
                            return models.toMutableList()
                        else if (args.size >= 2) {
                            val result = ArrayList<String>()
                            models.forEach { if (it.startsWith(args.last())) result.add(it) }
                            return result.toMutableList()
                        }
                    }
                    "set" -> {
                        if (!sender.isOp) {
                            return mutableListOf()
                        }
                        if (args.size == 1)
                            return varNames.toMutableList()
                        else if (args.size == 2) {
                            val result = ArrayList<String>()
                            varNames.forEach { if (it.startsWith(args[1])) result.add(it) }
                            return result.toMutableList()
                        }
                    }
                    "notice" -> {
                        if (!sender.isOp)
                            return mutableListOf()
                        val commands = mutableListOf("append", "remove", "broadcast")
                        if (args.size == 1) {
                            return if (args.first().isEmpty())
                                commands
                            else
                                commands.filter { it.startsWith(args.first()) }.toMutableList()
                        }
                    }
                }
            }
        } else if (command.name == "survey") {
            if (args.size == 1 && sender.isOp) {
                val commands = mutableListOf("pass", "rollback", "giveChance")
                return if (args.first().isEmpty()) {
                    commands
                } else {
                    val r = ArrayList<String>()
                    commands.forEach { if (it.startsWith(args.first())) r.add(it) }
                    r
                }
            } else if (args.size == 2 && sender.isOp) {
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
        } else if (command.name == "builder") {
            if (args.size == 1 && sender.isOp) {
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
            } else if (args.size == 2 && sender.isOp) {
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

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerRegister(event: PlayerRegisterEvent) {
        event.info.isSurveyPassed = false
    }

    @EventHandler
    fun onPlayerJoinGame(event: PlayerJoinGameEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (info == null) {
            event.player.error(Language.getDefault("player.error.unknown"))
            return
        }

        if (!info.isLogin) {
            event.isCancelled = true
            event.player.error(Language[info, "user.error.notLoginYet"])
        }

        if (info.isSurveyPassed)
            return
        val gamePlay = info.tag.getInt("gamePlayer", 0)
        if (gamePlay > 3) {
            event.isCancelled = true
            showPlayerOutOfDemoTitle(event.player)
            return
        }
        info.tag.set("gamePlayer", gamePlay + 1)
    }

    @EventHandler
    fun onServerListPing(event: PaperServerListPingEvent) {
        ServerHeaders.serverSelf.onServerPing.forEach {
            it.apply(arrayOf(SimpleServerListPingEvent(event)))
        }
    }
}