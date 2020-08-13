package com.zhufu.opencraft

import com.zhufu.opencraft.Base.endWorld
import com.zhufu.opencraft.Base.lobby
import com.zhufu.opencraft.Base.netherWorld
import com.zhufu.opencraft.Base.publicMsgPool
import com.zhufu.opencraft.Base.spawnWorld
import com.zhufu.opencraft.Base.surviveWorld
import com.zhufu.opencraft.Game.env
import com.zhufu.opencraft.PlayerManager.showPlayerOutOfDemoTitle
import com.zhufu.opencraft.chunkgenerator.VoidGenerator
import com.zhufu.opencraft.events.PlayerInventorySaveEvent
import com.zhufu.opencraft.events.PlayerJoinGameEvent
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.player_community.PlayerStatics
import com.zhufu.opencraft.special_item.base.SpecialItem
import com.zhufu.opencraft.survey.SurveyManager
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.MemoryNPCDataStore
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.DisplaySlot
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer
import kotlin.reflect.KFunction
import kotlin.system.measureTimeMillis

@Suppress("MemberVisibilityCanBePrivate")
class Core : JavaPlugin(), Listener {
    companion object {
        var npc = ArrayList<NPC>()
    }

    internal var reloadTask: BukkitTask? = null
    internal var saveTask: BukkitTask? = null
    private var awardTask: Timer? = null
    private lateinit var scoreBoardTask: BukkitTask
    override fun onEnable() {
        // Command
        getCommand("server")!!.apply {
            val e = ServerCommandExecutor(this@Core)
            setExecutor(e)
            tabCompleter = e
        }
        getCommand("survey")!!.apply {
            val e = SurveyCommandExecutor()
            setExecutor(e)
            tabCompleter = e
        }
        getCommand("builder")!!.apply {
            val e = ServerCommandExecutor(this@Core)
            setExecutor(e)
            tabCompleter = e
        }
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
        env.addDefaults(
            mapOf(
                "reloadDelay" to 2 * 60 * 20,
                "inventorySaveDelay" to 4 * 60 * 20,
                "prisePerBlock" to 10,
                "backToDeathPrise" to 3,
                "countOfSurveyQuestion" to 6,
                "secondsPerQuestion" to 30,
                "url" to "https://www.open-craft.cn",
                "debug" to false,
                "ssHotReload" to false,
                "survivalCenter" to surviveWorld.getHighestBlockAt(0, 0).location,
                "lobbyRadius" to 100
            )
        )
        handleTasks()

        with(Bukkit.getPluginManager()) {
            val self = this@Core
            registerEvents(self, self)
            registerEvents(SurviveListener(self), self)
        }

        if (!server.pluginManager.isPluginEnabled("Citizens")) {
            logger.warning("Citizens is not enabled.")
            logger.warning("Disabling NPC functionality.")
        } else {
            CitizensAPI.createNamedNPCRegistry("temp", MemoryNPCDataStore())
        }

        if (!dataFolder.exists()) dataFolder.mkdirs()
        runReport(ServerStatics::init)
        PlayerStatics.Companion
        runReport(SurveyManager::init, File(dataFolder, "survey.json"), this)
        runReport(this::ssInit)
        listOf(
            TaskManager::init,
            SpecialItem.Companion::init,
            GameManager::init,
            PlayerManager::init,
            TradeManager::init,
            PlayerObserverListener::init,
            PlayerLobbyManager::init,
            BuilderListener::init
        ).forEach {
            runReport(it, this)
        }

        ServerCaller["SolvePlayerLobby"] = {
            val info = (it.firstOrNull()
                ?: throw IllegalArgumentException("This call must be give at least one Info parameter.")) as Info
            PlayerLobbyManager[info].also { lobby -> if (!lobby.isInitialized) lobby.initialize() }.tpHere(info.player)
        }

        fun startAward() {
            fun Pair<Int, Int>.smaller() = if (first < second) first else second
            val yesterday = SimpleDateFormat("MM/dd").format(Date())

            awardTask = fixedRateTimer(
                name = "awardTask",
                startAt = Date(Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.DAY_OF_YEAR, get(Calendar.DAY_OF_YEAR) + 1)
                }.timeInMillis),
                period = 10
            ) {
                val chart = Game.dailyChart
                for (i in 0..(2 to chart.lastIndex).smaller()) {
                    val award = -300 * i + 1000
                    chart[i].apply {
                        messagePool.add(
                            text = "\${chart.award,$award}",
                            type = MessagePool.Type.System
                        ).apply {
                            recordTime()
                            if (isOnline)
                                sendTo(onlinePlayerInfo!!)
                        }
                        publicMsgPool.add(
                            text = "\$success\${chart.showOff,$name,$yesterday,${i + 1}}",
                            type = MessagePool.Type.OneTime,
                            extra = YamlConfiguration().apply {
                                set(name, true)
                            }
                        )
                        currency += award
                    }
                }
                startAward()
                this.cancel()
            }
        }
        startAward()
    }

    private fun runReport(f: KFunction<*>, vararg args: Any?) {
        try {
            f.call(*args)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDisable() {
        npc.forEach {
            it.despawn()
            it.destroy()
        }
        PlayerObserverListener.onServerStop()
        PlayerManager.forEachPlayer { it.saveServerID() }
        saveAll()
        SpecialItem.cleanUp()
        Scripting.cleanUp()
    }

    private fun saveAll() {
        logger.info("Saving everything...")
        env.save(File(dataFolder, "env"))
        listOf(
            TaskManager::save,
            SpecialItem.Companion::save,
            BuilderListener::saveConfig,
            PlayerStatics.Companion::saveAll,
            PlayerLobbyManager::saveAll,
            ServerStatics::save
        ).forEach {
            runReport(it)
        }
        publicMsgPool.serialize().save(Base.msgPoolFile)
    }

    internal var urlLooper = 0
    internal var looperDirection = true
    internal fun handleTasks() {
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
        }, 0, env.getLong("inventorySaveDelay"))

        reloadTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
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
                    info.statics!!.timeToday += 2
                    count = 1
                }

                if (info.status != Info.GameStatus.MiniGaming) {
                    var sort = 0

                    val newBoard = Bukkit.getScoreboardManager().newScoreboard
                    val obj = newBoard.registerNewObjective("serverStatics", "dummy", getter["server.statics.title"])
                    obj.displaySlot = DisplaySlot.SIDEBAR
                    obj.getScore(TextUtil.info(getter["server.statics.coinCount", info.currency])).score = --sort
                    obj.getScore(TextUtil.info(getter["server.statics.exp", info.exp])).score = --sort
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
                        ServerStatics.onlineTime += 2
                    }

                    // Tick SpecialItem
                    val modifier = PlayerModifier(info)
                    if (info.player.inventory.containsSpecialItem) {
                        val data = YamlConfiguration()
                        info.player.inventory.specialItems.forEach { item ->
                            item.doPerTick(modifier, data, obj, --sort)
                        }
                    }
                    modifier.apply()
                    // URL Loop
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
                        if (urlLooper >= url!!.lastIndex || (urlLooper <= 0 && !looperDirection)) {
                            looperDirection = !looperDirection
                        }
                        urlLooper += if (looperDirection) 1 else -1
                    }
                    info.player.scoreboard = newBoard
                }
            }
        }, 0L, 1)
    }

    fun ssInit() {
        Scripting
        val ssLoadTime = measureTimeMillis {
            Scripting.cleanUp()
            Scripting.init(this).let { failures ->
                if (failures.isEmpty()) return@let
                logger.warning {
                    buildString {
                        append("Failed to load following server scripts: ")
                        failures.forEach {
                            append(it.nameWithoutExtension)
                            append(", ")
                        }
                    }.removeSuffix(", ")
                }
            }
        }
        if (env.getBoolean("debug")) {
            logger.info("ServerScript initialization finished in ${ssLoadTime}ms.")
        }
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
    fun onServeReload(event: ServerReloadEvent) {
        saveAll()
    }
}