package com.zhufu.opencraft

import com.zhufu.opencraft.Base.endWorld
import com.zhufu.opencraft.Base.lobby
import com.zhufu.opencraft.Base.netherWorld
import com.zhufu.opencraft.Base.publicMsgPool
import com.zhufu.opencraft.Base.spawnWorld
import com.zhufu.opencraft.Base.surviveWorld
import com.zhufu.opencraft.Game.env
import com.zhufu.opencraft.Game.varNames
import com.zhufu.opencraft.PlayerManager.showPlayerOutOfDemoTitle
import com.zhufu.opencraft.chunkgenerator.VoidGenerator
import com.zhufu.opencraft.events.PlayerInventorySaveEvent
import com.zhufu.opencraft.events.PlayerJoinGameEvent
import com.zhufu.opencraft.listener.NPCListener
import com.zhufu.opencraft.listener.NPCSelectListener
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.player_community.PlayerStatics
import com.zhufu.opencraft.special_item.base.SpecialItem
import com.zhufu.opencraft.survey.SurveyManager
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.SpawnReason
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Equipment
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.util.Vector
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer
import kotlin.reflect.KFunction

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

        surviveWorld.apply {
            setGameRule(GameRule.KEEP_INVENTORY, false)
            difficulty = Difficulty.HARD
        }
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
            if (server.pluginManager.isPluginEnabled("Citizens")) {
                registerEvents(NPCListener, self)
                registerEvents(NPCSelectListener(npc.toTypedArray()), self)
            }
            registerEvents(SurviveListener(self), self)
        }
        if (!dataFolder.exists()) dataFolder.mkdirs()
        runReport(ServerStatics::init)
        PlayerStatics.Companion
        runReport(SurveyManager::init, File(dataFolder, "survey.json"), this)
        listOf(
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
        if (!server.pluginManager.isPluginEnabled("Citizens")) {
            logger.warning("Citizens is not enabled.")
            logger.warning("Disabling NPC functionality.")
        } else {
            Bukkit.getScheduler().runTaskLater(this, { _ ->
                spawnNPC()
            }, 40)
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
            logger.throwing("ServerCore", f.name, e)
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
    }

    private fun saveAll() {
        logger.info("Saving everything...")
        env.save(File(dataFolder, "env"))
        listOf(
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
                    info.statics!!.timeToday += 2
                    count = 1
                }

                if (info.status != Info.GameStatus.MiniGaming) {
                    var sort = 0

                    val newBoard = Bukkit.getScoreboardManager().newScoreboard
                    val obj = newBoard.registerNewObjective("serverStatics", "dummy", getter["server.statics.title"])
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
                        ServerStatics.onlineTime += 2
                    }


                    val modifier = PlayerModifier(info)
                    if (info.player.inventory.containsSpecialItem) {
                        val data = YamlConfiguration()
                        info.player.inventory.specialItems.forEach { item ->
                            item.doPerTick(modifier, data, obj, --sort)
                        }
                    }
                    modifier.apply()

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

    internal fun spawnNPC() {
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