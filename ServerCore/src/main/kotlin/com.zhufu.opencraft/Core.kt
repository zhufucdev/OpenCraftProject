package com.zhufu.opencraft

import com.zhufu.opencraft.Base.Extend.isDigit
import com.zhufu.opencraft.Base.endWorld
import com.zhufu.opencraft.Base.lobby
import com.zhufu.opencraft.Base.netherWorld
import com.zhufu.opencraft.Base.publicMsgPool
import com.zhufu.opencraft.Base.spawnWorld
import com.zhufu.opencraft.Base.surviveWorld
import com.zhufu.opencraft.Game.env
import com.zhufu.opencraft.Game.varNames
import com.zhufu.opencraft.api.ServerCaller
import com.zhufu.opencraft.chunkgenerator.VoidGenerator
import com.zhufu.opencraft.data.Database
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.data.OfflineInfo
import com.zhufu.opencraft.events.PlayerInventorySaveEvent
import com.zhufu.opencraft.events.PlayerJoinGameEvent
import com.zhufu.opencraft.listener.*
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import com.zhufu.opencraft.player_community.FriendshipImpl
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.player_community.PlayerStatics
import com.zhufu.opencraft.player_community.PublicMessagePool
import com.zhufu.opencraft.special_item.StatefulSpecialItem
import com.zhufu.opencraft.survey.SurveyManager
import com.zhufu.opencraft.util.*
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.SpawnReason
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Equipment
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.util.Vector
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToLong
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions
class Core : JavaPlugin(), Listener {
    companion object {
        var npc = ArrayList<NPC>()
    }

    private val envFile get() = File(dataFolder, "env")

    private var reloadTask: BukkitTask? = null
    private var inventoryTask: BukkitTask? = null
    private var awardTask: Timer? = null
    private var scoreBoardTask: BukkitTask? = null
    override fun onEnable() {
        //World initializations
        spawnWorld = server.createWorld(
            WorldCreator.name("world_spawn")
                .type(WorldType.FLAT)
                .generator(VoidGenerator(2))
        )!!
        spawnWorld.peace()
        surviveWorld = server.createWorld(WorldCreator("world_survive"))!!
        netherWorld = server.getWorld("world_nether")!!
        endWorld = server.getWorld("world_the_end")!!
        lobby = server.getWorld("world")!!
        lobby.peace()

        surviveWorld.apply {
            setGameRule(GameRule.KEEP_INVENTORY, false)
            difficulty = Difficulty.HARD
        }
        env =
            try {
                YamlConfiguration.loadConfiguration(envFile.also {
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
            registerEvents(SurviveListener(self), self)
        }
        if (!dataFolder.exists()) dataFolder.mkdirs()
        runInit(Database::init, env.getString("database"))
        ServerStatics
        PlayerStatics.Companion
        runInit(SurveyManager::init, File(dataFolder, "survey.json"), this)
        listOf(
            GameManager::init,
            PlayerManager::init,
            TradeManager::init,
            PlayerObserverListener::init,
            PlayerLobbyManager::init,
            BuilderListener::init,
            SpecialItemHandle::init,
            SpecialBlockHandler::init
        ).forEach {
            runInit(it, this)
        }

        ServerCaller["SolvePlayerLobby"] = {
            val info = (it.firstOrNull()
                ?: throw IllegalArgumentException("This call must be give at least one Info parameter.")) as Info
            PlayerLobbyManager[info].also { lobby -> if (!lobby.isInitialized) lobby.initialize() }.visitBy(info.player)
        }

        scheduleAwards()
        Bukkit.getPluginManager().callEvent(CoreInitializedEvent(this))
    }

    private fun scheduleAwards() {
        fun Pair<Int, Int>.smaller() = if (first < second) first else second
        val yesterday = SimpleDateFormat("MM/dd").format(Date())
        val r = Date.from(
            LocalDate.now(Base.timeZone.toZoneId())
                .atStartOfDay()
                .plusDays(1)
                .atZone(Base.timeZone.toZoneId())
                .toInstant()
        )
        logger.info("Next award will be carried at ${DateFormat.getInstance().format(Date.from(r.toInstant()))}.")
        awardTask = fixedRateTimer(
            name = "awardTask",
            startAt = r,
            period = Duration.ofDays(2).toMillis()
        ) {
            val chart = Game.dailyChart
            for (i in 0..(2 to chart.lastIndex).smaller()) {
                val award = -30 * i + 100
                chart[i].apply {
                    messagePool.add(
                        text = "\${chart.award,$award}",
                        type = MessagePool.Type.System
                    ).apply {
                        recordTime()
                        if (isOnline)
                            sendTo(onlinePlayerInfo!!)
                    }
                    val id = PublicMessagePool.add(
                        text = "\$success\${chart.showOff,$name,$yesterday,${i + 1}}",
                        type = MessagePool.Type.OneTime
                    ).id
                    PublicMessagePool.markAsRead(id, this)
                    currency += award
                }
            }
            scheduleAwards()
            this.cancel()
        }
    }

    private fun runInit(f: KFunction<*>, vararg args: Any?) {
        try {
            f.call(*args)
        } catch (e: Exception) {
            logger.warning("Error while initializing ${f.name}: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onDisable() {
        npc.forEach {
            it.despawn()
            it.destroy()
        }
        PlayerObserverListener.onServerStop()
        saveAll()
        listOf(reloadTask, inventoryTask, scoreBoardTask).forEach { it?.cancel() }
        awardTask?.cancel()
        Database.close()
    }

    private fun saveSharedInventory() {
        FriendshipImpl.cached.forEach {
            try {
                it.serializeInventory()
                it.update()
            } catch (e: Exception) {
                logger.warning("Error while saving shared inventory between ${it.a.name} and ${it.b.name}.")
                e.printStackTrace()
            }
        }
    }

    private fun saveAll() {
        Bukkit.getLogger().info("Saving everything...")
        env.save(envFile)
        BuilderListener.saveConfig()
        PlayerLobbyManager.saveAll()
        saveSharedInventory()
    }

    private var urlLooper = 0
    private var loopRight = true
    private fun handleTasks() {
        env.addDefaults(
            mapOf(
                "reloadDelay" to 2 * 60 * 20,
                "inventorySaveDelay" to 4 * 60 * 20,
                "prisePerBlock" to 10,
                "backToDeathPrise" to 3,
                "countOfSurveyQuestion" to 6,
                "secondsPerQuestion" to 30,
                "url" to "https://www.open-craft.cn",
                "database" to "mongodb://localhost:27017",
                "debug" to false,
                "ssHotReload" to false
            )
        )
        env.save(envFile)
        inventoryTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            Bukkit.getPluginManager().callEvent(PlayerInventorySaveEvent())
            PlayerManager.forEachPlayer {
                if (it.status == Info.GameStatus.Surviving) {
                    try {
                        it.inventory.present.sync()
                        it.player.info(Language[it, "server.inventory.saved"])
                    } catch (e: Exception) {
                        logger.warning("Error while saving inventory for player ${it.player.name}")
                        e.printStackTrace()
                        it.player.error(Language[it, "server.inventory.error", e.message])
                    }
                }
            }
        }, 0, env.getLong("inventorySaveDelay"))

        reloadTask = Bukkit.getScheduler().runTaskTimer(this, Runnable {
            Bukkit.getPluginManager().callEvent(ServerReloadEvent())
        }, 0, env.getLong("reloadDelay"))

        var count = 1
        scoreBoardTask = Bukkit.getScheduler().runTaskTimer(this, Runnable {
            count++
            val url = env.getString("url")
            PlayerManager.forEachPlayer { info ->
                val getter = getLangGetter(info)
                val isRound = count % 40 == 0
                if (isRound) {
                    info.statics.timeToday += 2
                    count = 1
                }

                if (info.status != Info.GameStatus.MiniGaming) {
                    var sort = 0

                    val newBoard = Bukkit.getScoreboardManager().newScoreboard
                    val obj = newBoard.registerNewObjective(
                        "serverStatics",
                        Criteria.DUMMY,
                        getter["server.statics.title"].toComponent()
                    )
                    obj.displaySlot = DisplaySlot.SIDEBAR
                    obj.getScore(TextUtil.info(getter["server.statics.coinCount", info.currency])).score = --sort
                    if (!info.isSurveyPassed) {
                        obj.getScore(TextUtil.info(getter["server.statics.demoTime", info.remainingDemoTime / 1000L / 60]))
                            .score = --sort
                        if (info.remainingDemoTime <= 0 && info.status != Info.GameStatus.InLobby) {
                            PlayerManager.onPlayerOutOfDemo(info)
                        }
                    }
                    if (info.isInBuilderMode) {
                        obj.getScore(
                            TextUtil.getColoredText(
                                getter["server.statics.builderLevel", info.builderLevel],
                                TextUtil.TextColor.BLUE,
                                false,
                                underlined = false
                            )
                        ).score = --sort
                    }

                    if (isRound && info.status != Info.GameStatus.InLobby && info.status != Info.GameStatus.InTutorial) {
                        info.gameTime += 2 * 1000L
                    }

                    if (info.player.inventory.containsSpecialItem) {
                        val modifier = PlayerModifier(info)
                        val data = YamlConfiguration()
                        info.player.inventory.specialItems.forEach { item ->
                            if (item.holder != info.player) {
                                item.holder = info.player
                                item.updateMeta(getter)
                                info.player.inventory.setItem(item.inventoryPosition, item)
                            }
                            if (item is StatefulSpecialItem)
                                item.tick(modifier, data, obj, --sort)
                        }
                        modifier.apply()
                    }

                    obj.getScore(
                        buildString {
                            val all = TextUtil.TextColor.AQUA.code
                            val special = TextUtil.TextColor.WHITE.code
                            append(url)
                            insert(urlLooper, special)
                            insert(urlLooper + 3, all)
                            insert(0, all)
                        }
                    ).score = --sort
                    if (isRound) {
                        if (urlLooper >= url!!.lastIndex || (urlLooper <= 0 && !loopRight)) {
                            loopRight = !loopRight
                        }
                        urlLooper += if (loopRight) 1 else -1
                    }
                    info.player.scoreboard = newBoard
                }
            }
        }, 0L, 1)
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
                        sender.error("您没有输入文本")
                        return false
                    }
                    val sb = StringBuilder()
                    for (i in 1 until args.size)
                        sb.append("${args[i]} ")
                    sb.deleteCharAt(sb.length - 1)

                    if (sb.length > 300) {
                        sender.error("字数超过上限(300字)")
                        return true
                    }

                    val file = File(
                        "$dataFolder${File.separatorChar}feedback${File.separatorChar}${
                            SimpleDateFormat("yyyy${File.separatorChar}MM${File.separatorChar}dd${File.separatorChar}HH:mm").format(
                                Date()
                            )
                        }-${sender.name}.txt"
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

                    sender.info("您的反馈已提交，${TextUtil.success("感谢支持")}")
                }

                args.first() == "about" -> {
                    val readme = File(dataFolder, "readme.txt")
                    if (!readme.exists()) {
                        sender.info("未找到自述文件")
                        return true
                    }
                    val charset = if (args.size >= 2) try {
                        Charset.forName(args[1])
                    } catch (e: Exception) {
                        sender.error("未找到编码格式: ${args[1]}")
                        return false
                    } else Charsets.UTF_8
                    Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
                        val path = Paths.get(
                            System.getenv("PATH").split(':').firstOrNull {
                                Paths.get(it, "screenfetch").toFile().exists()
                            } ?: "null",
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
                                .replace("[37m", "${ChatColor.COLOR_CHAR}${ChatColor.WHITE}")
                                .plus(ChatColor.RESET.char)
                        } else "ERROR: no shell command screenfetch."
                        sender.sendMessage(
                            *TextUtil.format(
                                title = "自述文件",
                                content = screenfetch + readme.readText(charset)
                            )
                        )
                    })
                }

                args.first() == "stop" -> {
                    if (sender !is ConsoleCommandSender && !sender.isOp) {
                        sender.error("您没有权限使用此命令")
                        return true
                    }
                    if (args.size < 2) {
                        sender.error("请给出原因")
                        return false
                    }
                    val message = args[1].toInfoMessage()
                    val title = Title.title(
                        message,
                        Component.empty(),
                        Title.Times.times(
                            Duration.ofMillis(500),
                            Duration.ofDays(2),
                            Duration.ZERO
                        )
                    )
                    server.onlinePlayers.forEach {
                        it.showTitle(title)
                    }
                    Bukkit.getScheduler().runTaskLater(this, Runnable {
                        Bukkit.getOnlinePlayers().forEach {
                            it.kick(message)
                        }
                        server.shutdown()
                    }, (args[1].length * 0.2 * 20).roundToLong())
                }

                args.first() == "reload" -> {
                    if (sender !is ConsoleCommandSender && !sender.isOp) {
                        sender.error("您没有权限使用此命令")
                        return true
                    }
                    if (args.size < 2) {
                        Bukkit.getPluginManager().callEvent(ServerReloadEvent())
                        sender.info("正在重载服务器")
                        return true
                    }

                    val failure = ArrayList<String>()
                    (1 until args.size).forEach {
                        var result = false
                        when (args[it]) {
                            "game" -> {
                                sender.info("正在重载游戏")
                                result = true
                                GameManager.forEach { game ->
                                    val id = game.gameID
                                    try {
                                        game.initGameEnder()
                                    } catch (e: Exception) {
                                        result = false
                                        sender.error("gameID: $id ${e.javaClass.simpleName}: ${e.message}")
                                        e.printStackTrace()
                                    }
                                }
                            }

                            "survey" -> {
                                sender.info("正在重载服务器调查")
                                SurveyManager.init(File(dataFolder, "survey.json"), null)
                                result = true
                            }

                            "lang" -> {
                                sender.info("正在重载语言文件")
                                result = try {
                                    Language.init()
                                    true
                                } catch (e: Exception) {
                                    sender.error("${e.javaClass.simpleName}: ${e.message}")
                                    e.printStackTrace()
                                    false
                                }
                            }

                            "ss" -> {
                                sender.info("正在重载脚本")
                                try {
                                    val plugin = Bukkit.getPluginManager().getPlugin("ServerScript")
                                    if (plugin != null) {
                                        plugin::class.functions.first { kFunction -> kFunction.name == "initScripting" }
                                            .call(plugin)
                                        result = true
                                    } else {
                                        Bukkit.getLogger().warning("ServerScript plugin isn't loaded")
                                    }
                                } catch (e: Exception) {
                                    sender.error("${e.javaClass.simpleName}: ${e.message}")
                                    e.printStackTrace()
                                }
                            }

                            "builder" -> {
                                sender.info("正在重载建筑者配置")
                                result = try {
                                    reloadConfig()
                                    true
                                } catch (e: Exception) {
                                    sender.error("${e.javaClass.simpleName}: ${e.message}")
                                    e.printStackTrace()
                                    false
                                }
                            }

                            "database" -> {
                                sender.info("正在重载数据库")
                                result = try {
                                    Database.init(env.getString("database")!!)
                                    true
                                } catch (e: Exception) {
                                    sender.error("${e.javaClass.simpleName}: ${e.message}")
                                    e.printStackTrace()
                                    false
                                }
                            }

                            else -> {
                                sender.error("无效模块: ${args[1]}")
                            }
                        }
                        if (!result)
                            failure.add(args[it])
                    }
                    sender.sendMessage(
                        if (failure.isEmpty())
                            "成功重载指定模块".toSuccessMessage()
                        else {
                            "无法重载${failure.joinToString()}".toErrorMessage()
                        }
                    )
                }

                args.first() == "set" -> {
                    if (!sender.isOp) {
                        sender.error("您没有权限使用此命令")
                        return true
                    }
                    if (args.size < 3) {
                        sender.error("用法错误")
                        return true
                    }
                    when (args[1]) {
                        "url" -> {
                            env.set("url", args[2])
                            env.save(envFile)
                            sender.success("已将服务器网站地址标记为${args[2]}")
                            urlLooper = 0
                            loopRight = true
                        }

                        "database" -> {
                            env.set("database", args[2])
                            env.save(envFile)
                            sender.success("已将服务器数据库修改为${args[2]}")
                            sender.tip("重载数据库以生效")
                        }

                        "debug" -> {
                            if (args[2] == "on") {
                                env.set("debug", true)
                                sender.success("已启动调试模式")
                            } else {
                                env.set("debug", false)
                                sender.success("已关闭调试模式")
                            }
                        }

                        "ssHotReload" -> {
                            val content = "服务器脚本动态刷新，重载服务器脚本以生效"
                            if (args[2] == "true") {
                                env.set("ssHotReload", true)
                                sender.success("已启用$content")
                            } else {
                                env.set("ssHotReload", false)
                                sender.success("已关闭$content")
                            }
                        }

                        else -> {
                            fun checkIntOverZero(): Boolean {
                                if (!args[2].isDigit()) {
                                    sender.error("此变量只允许数字")
                                    return false
                                }
                                if (args[2].contains('.') || args[2].contains('-')) {
                                    sender.error("此变量只允许正整数")
                                    return false
                                }
                                return true
                            }
                            if (!varNames.contains(args[1])) {
                                sender.error("变量不存在")
                                return true
                            }
                            if (checkIntOverZero()) {
                                env.set(args[1], args[2].toIntOrNull())
                                sender.success("已将服务器环境变量${args[1]}设置为${args[2]}")

                                reloadTask?.cancel()
                                handleTasks()
                            }
                        }
                    }
                    env.save(envFile)
                }

                args.first() == "notice" -> {
                    if (args.size <= 1 && sender is Player && sender.isOp) {
                        publicMsgPool.sendAllTo(sender.info() ?: return true)
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
                            PublicMessagePool.add(
                                text = text,
                                type = MessagePool.Type.System
                            )
                            sender.success("已在公共消息池中追加该内容")
                            PlayerManager.forEachChatter { publicMsgPool.sendUnreadTo(it) }
                        }

                        "remove" -> {
                            val id = args[2].toIntOrNull()
                            if (id == null || id < 0) {
                                sender.error("参数必须是自然数")
                                return true
                            }
                            PublicMessagePool.remove(id)
                            sender.success("已移除索引为${id}的消息")
                        }

                        "broadcast" -> PlayerManager.forEachChatter { publicMsgPool.sendUnreadTo(it) }
                    }
                }

                args.first() == "script" -> {
                    val getter = sender.getter()
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
                    TODO()
                    /*
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
                     */
                }
            }
        } else if (command.name == "survey") {
            if (args.isNotEmpty()) {
                if (!sender.isOp) {
                    sender.error("您没有权限使用该命令")
                    return true
                }
                if (args.size < 2) {
                    sender.error("用法错误")
                    return true
                }
                when (args.first()) {
                    "pass" -> {
                        val it = PlayerManager.findOfflineInfoByPlayer(Bukkit.getOfflinePlayer(args[1]).uniqueId)
                        if (it == null) {
                            sender.error("找不到玩家")
                            return true
                        }
                        if (it.isOnline) {
                            it.onlinePlayerInfo!!.player.showTitle(
                                Title.title(
                                    Component.empty(),
                                    "您已被管理员给予正式会员的身份".toInfoMessage(),
                                    Title.Times.times(
                                        Duration.ofMillis(150),
                                        Duration.ofSeconds(3),
                                        Duration.ofMillis(150)
                                    )
                                )
                            )
                        }
                        it.isSurveyPassed = true
                    }

                    "rollback" -> {
                        val it = PlayerManager.findOfflineInfoByPlayer(Bukkit.getOfflinePlayer(args[1]).uniqueId)
                        if (it == null) {
                            sender.error("找不到玩家")
                            return true
                        }
                        if (it.isOnline) {
                            it.onlinePlayerInfo!!.player.showTitle(
                                Title.title(
                                    Component.empty(),
                                    "您已被管理员剥夺正式会员的身份".toInfoMessage(),
                                    Title.Times.times(
                                        Duration.ofMillis(150),
                                        Duration.ofSeconds(3),
                                        Duration.ofMillis(150)
                                    )
                                )
                            )
                        }
                        it.isSurveyPassed = false
                    }

                    "giveChance" -> {
                        val it = PlayerManager.findOfflineInfoByPlayer(Bukkit.getOfflinePlayer(args[1]).uniqueId)
                        if (it == null) {
                            sender.error("找不到玩家")
                            return true
                        }
                        val num = args[2].toIntOrNull()
                        if (num == null) {
                            sender.error("非法参数 ${args[2]}: 参数不是整数")
                            return true
                        }
                        it.remainingSurveyChance += num
                        if (it.isOnline) {
                            it.onlinePlayerInfo!!.player.info("您被给予${num}次参与服务器调查的机会，您现在有${it.remainingSurveyChance}次机会")
                        }
                    }
                }
            } else {
                if (sender !is Player) {
                    sender.error("只有玩家才能使用此命令")
                    return true
                }
                SurveyManager.startSurvey(sender)
            }
        } else if (command.name == "builder") {
            if (args.isEmpty()) {
                if (sender !is Player) {
                    sender.error("只有玩家才能使用此命令")
                    return true
                }
                val info = PlayerManager.findInfoByPlayer(sender)
                if (info == null) {
                    sender.error(Language.getDefault("player.error.unknown"))
                    return true
                }
                if (sender.info()?.isBuilder != true) {
                    sender.error("只有建筑者才能使用此命令")
                    return true
                }

                if (info.status != Info.GameStatus.MiniGaming && info.status != Info.GameStatus.InTutorial && info.status != Info.GameStatus.Observing) {
                    BuilderListener.switch(sender)
                } else {
                    sender.error("抱歉，但您不能在此时使用此命令")
                    return true
                }
            } else if (args.size >= 2) {
                if (!sender.isOp) {
                    sender.error("您没有权限使用此命令")
                    return true
                }
                val player = Bukkit.getOfflinePlayer(args[1])
                fun notify(c: Component) {
                    player.player?.showTitle(
                        Title.title(
                            Component.empty(),
                            c,
                            Title.Times.times(
                                Duration.ofMillis(150),
                                Duration.ofSeconds(3),
                                Duration.ofMillis(150)
                            )
                        )
                    )
                }
                when (args.first()) {
                    "pass" -> {
                        if (player.offlineInfo()?.isBuilder != true) {
                            BuilderListener.updatePlayerLevel(player, 3)
                            sender.success("以给予${args.last()}建筑者的身份")
                            notify("您已被管理员给予建筑者的身份".toInfoMessage())
                        }
                    }

                    "rollback" -> {
                        if (player.offlineInfo()?.isBuilder == true) {
                            BuilderListener.updatePlayerLevel(player, 0)
                            sender.success("以夺去${args.last()}建筑者的身份")
                            notify("您已被管理员夺去建筑者的身份".toInfoMessage())
                        }
                    }

                    "set" -> {
                        val num = args[2].toIntOrNull()
                        if (num == null || num < 1) {
                            sender.error("无效参数: ${args[2]}: 参数不是自然数")
                            return true
                        }
                        BuilderListener.updatePlayerLevel(player, num)
                        sender.success("已将${player.name}的建筑者等级设置为$num")
                        notify("您的建筑者等级已更新为$num".toInfoMessage())
                    }

                    else -> {
                        sender.error("用法错误")
                    }
                }
            } else {
                sender.error("用法错误")
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
                        val models = listOf("game", "notice", "survey", "lang", "ss", "builder")
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

                        when (args.size) {
                            1 -> return varNames.toMutableList()
                            2 -> {
                                val result = ArrayList<String>()
                                varNames.forEach { if (it.startsWith(args[1])) result.add(it) }
                                return result.toMutableList()
                            }

                            3 -> {
                                if (args.last() == "debug")
                                    return mutableListOf("true", "false")
                            }
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
        } else if (command.name == "builder") {
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
        }
        return mutableListOf()
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
    }

    @EventHandler
    fun onServeReload(event: ServerReloadEvent) {
        saveAll()
        ServerStatics.record()
    }
}

