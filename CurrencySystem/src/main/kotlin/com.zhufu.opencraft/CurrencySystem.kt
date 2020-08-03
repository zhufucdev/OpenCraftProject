package com.zhufu.opencraft

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.zhufu.opencraft.Base.TutorialUtil.gmd
import com.zhufu.opencraft.Base.TutorialUtil.tplock
import com.zhufu.opencraft.Base.TutorialUtil.linearTo
import com.zhufu.opencraft.Base.tradeWorld
import com.zhufu.opencraft.Base.Extend.toPrettyString
import com.zhufu.opencraft.inventory.TraderInventory
import com.zhufu.opencraft.Base.Extend.isDigit
import com.zhufu.opencraft.Game.env
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Equipment
import net.citizensnpcs.api.util.MemoryDataKey
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.generator.ChunkGenerator
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CurrencySystem : JavaPlugin() {
    companion object {
        var npc: NPC? = null
        var npcBack: NPC? = null
        val transMap = HashMap<Material, Long>()
        val inventoryMap = arrayListOf<TraderInventory>()
        val territoryMap = arrayListOf<TradeTerritoryInfo>()
        val tradeRoot = Paths.get("plugins", "trade").toFile()
        val donation: File
            get() = File(tradeRoot, "donation.png")

        var server: ServerSocket? = null
        var checker: ServerSocket? = null
        var client: Socket? = null
        private val threadPool = Executors.newCachedThreadPool()
        val isServerReady: Boolean
            get() {
                val connection = FutureTask {
                    var result: Boolean

                    if (checker == null || checker!!.isClosed) {
                        checker = ServerSocket(2334)
                    }
                    try {
                        val client = checker!!.accept()
                        val input = client.getInputStream().bufferedReader()
                        val json = JsonParser().parse(input.readLine()).asJsonObject
                        client.shutdownInput()
                        result = password.isNotEmpty() && json.has("pwd") && json["pwd"].asString == password

                        val back = JsonObject()
                        back.addProperty("result", if (result) 0 else 1)
                        val writer = PrintWriter(client.getOutputStream())
                        writer.print(back.toString())
                        writer.flush()
                        client.shutdownOutput()
                        client.close()
                    } catch (e: Exception) {
                        result = false
                    }
                    result
                }
                threadPool.execute(connection)
                return try {
                    connection[2, TimeUnit.SECONDS]
                } catch (e: TimeoutException) {
                    false
                }
            }
        var password = ""

        lateinit var instance: CurrencySystem

        fun showTutorial(player: Player) {
            Bukkit.getScheduler().runTaskAsynchronously(Base.pluginCore, Runnable {
                val info = PlayerManager.findInfoByPlayer(player)
                if (info == null) {
                    player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
                    return@Runnable
                }
                info.status = Info.GameStatus.InTutorial
                val getter = Language[info]
                player.gmd(GameMode.SPECTATOR)
                player.tplock(
                    tradeWorld.spawnLocation
                        .add(Vector(0, 30, 0))
                        .setDirection(Vector(0, -90, 0)),
                    5 * 1000L
                )
                player.sendTitle(
                    TextUtil.error(getter["trade.tutorial.1.title"]),
                    TextUtil.getColoredText(getter["trade.tutorial.1.subtitle"], TextUtil.TextColor.AQUA),
                    7, 5 * 20, 7
                )
                Thread.sleep(5 * 1000L + 50)

                val l1 = Location(tradeWorld, 7.5, 62.0, 6.5)
                    .setDirection(Vector(0, 0, -90))
                player.tplock(l1, 7 * 1000L)
                player.sendTitle(
                    TextUtil.tip(getter["trade.tutorial.2.title"]),
                    TextUtil.getColoredText(getter["trade.tutorial.2.subtitle"], TextUtil.TextColor.AQUA),
                    7, 7 * 20, 7
                )
                Thread.sleep(7 * 1000L + 50)


                val l2 = territoryMap.firstOrNull { it.player == player.uniqueId }
                if (l2 == null)
                    player.sendMessage(TextUtil.error(getter["trade.error.chunkNotFound"]))
                else {
                    val center = l2.center
                    val L2 =
                        Location(tradeWorld, center.x.toDouble(), tradeWorld.spawnLocation.y + 30, center.z.toDouble())
                            .setDirection(Vector(0, -90, 0))
                    player.tplock(L2, 7 * 1000L)
                    player.sendTitle(
                        TextUtil.tip(getter["trade.tutorial.3.title"]),
                        TextUtil.getColoredText(getter["trade.tutorial.3.subtitle"], TextUtil.TextColor.AQUA),
                        7, 3 * 20, 0
                    )
                    Thread.sleep(3 * 1000L)
                    player.sendTitle(
                        TextUtil.tip(getter["trade.tutorial.3.title"]),
                        TextUtil.getColoredText(getter["trade.tutorial.4.subtitle"], TextUtil.TextColor.AQUA),
                        0, 4 * 20, 7
                    )
                    Thread.sleep(4 * 1000L + 50)
                }
                val l3Top = tradeWorld.spawnLocation.clone()
                    .add(Vector(0, 30, 0))
                    .setDirection(Vector(0, -90, 0))
                val l3Bottom = l3Top.clone().add(Vector(0.0, -15.0, 0.0))
                val scheduler = Bukkit.getScheduler()
                scheduler.runTask(instance) { _ ->
                    player.teleport(l3Top)
                }
                player.sendTitle(
                    TextUtil.tip(getter["trade.tutorial.5.title"]),
                    TextUtil.getColoredText(getter["trade.tutorial.5.subtitle"], TextUtil.TextColor.AQUA), 7, 4 * 20, 0
                )
                player.linearTo(l3Bottom, 13 * 1000L, 20)
                Thread.sleep(4 * 1000L + 50)
                player.sendTitle(
                    TextUtil.tip(getter["trade.tutorial.5.title"]),
                    TextUtil.getColoredText(getter["trade.tutorial.6.subtitle"], TextUtil.TextColor.AQUA), 0, 4 * 20, 0
                )
                Thread.sleep(4 * 1000L + 50)
                player.sendTitle(
                    TextUtil.tip(getter["trade.tutorial.5.title"]),
                    TextUtil.getColoredText(getter["trade.tutorial.7.subtitle"], TextUtil.TextColor.AQUA), 0, 6 * 20, 7
                )
                Thread.sleep(6 * 1000L + 50)

                player.linearTo(tradeWorld.spawnLocation, 1500L)
                Thread.sleep(1500L)

                player.gmd(GameMode.ADVENTURE)
                player.sendTitle(
                    TextUtil.getColoredText(getter["tutorial.begin"], TextUtil.TextColor.RED, true),
                    "",
                    7,
                    60,
                    7
                )

                PlayerManager.findInfoByPlayer(player)
                    ?.also { it.status = Info.GameStatus.Surviving }
                    ?.tag?.set("isTradeTutorialShown", true)
                    ?: player.sendMessage(TextUtil.warn(Language.getDefault("player.error.unknown")))
            })
        }
    }

    private fun showHelp(): String = File(dataFolder, "help.txt")
        .also { if (!it.parentFile.exists()) it.parentFile.mkdirs() }
        .readText()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "trade") {
            if (sender !is Player && args.size < 3 && args.first() != "pwd") {
                sender.sendMessage(TextUtil.error(getLang(sender, "command.error.playerOnly")))
                return true
            }
            if (args.isEmpty()) {
                sender.sendMessage(TextUtil.error(getLang(sender, "command.error.usage")))
                return true
            }

            val getter = sender.getter()
            fun checkLogin(player: Player): Boolean {
                if (player.info()?.isLogin == false) {
                    player.error(getter["user.error.notLoginYet"])
                    return false
                }
                return true
            }
            when (args.first()) {
                "back" -> {
                    val player = sender as Player
                    if (!checkLogin(player)) return true
                    val t = territoryMap.firstOrNull { it.player == player.uniqueId }
                    if (t == null) {
                        sender.sendMessage(TextUtil.error(getter["trade.error.chunkNotFound"]))
                        return true
                    }
                    val dest = t.center
                    val event = PlayerTeleportedEvent(sender, sender.location, dest)
                    server.pluginManager.callEvent(event)
                    if (!event.isCancelled) {
                        sender.teleport(dest)
                    }
                }
                "center" -> {
                    val player = sender as Player
                    if (!checkLogin(player)) return true
                    val event = PlayerTeleportedEvent(player, sender.location, tradeWorld.spawnLocation)
                    server.pluginManager.callEvent(event)
                    if (!event.isCancelled)
                        player.teleport(tradeWorld.spawnLocation)
                }
                "help" -> {
                    sender.sendMessage(TextUtil.format(showHelp(), getter["trade.help.title"]))
                }
                "give" -> {
                    if (!sender.isOp) {
                        sender.error(getter["command.error.permission"])
                        return true
                    }
                    if (args.size < 3) {
                        sender.error(getter["command.error.usage"])
                        return true
                    }
                    val player = Bukkit.getPlayer(args[1])
                    if (player == null) {
                        sender.error(getter["command.error.playerNotFound"])

                    } else {
                        val info = PlayerManager.findInfoByPlayer(player)
                        if (info == null) {
                            sender.error(getter["command.error.playerNotFound"])
                        } else if (!args[2].isDigit()) {
                            sender.error(getter["command.error.argNonDigit"])
                        } else {
                            info.currency += args[2].toInt()

                            sender.info(getter["trade.giveCurrency.objective", player.name, args[2]])
                            player.info(getLang(info, "trade.giveCurrency.subjective", args[2], info.currency))
                        }
                    }
                }
                "set" -> {
                    if (!sender.isOp) {
                        sender.error(getter["command.error.permission"])
                    } else if (args.size < 3) {
                        sender.error(getter["command.error.usage"])
                        return true
                    }
                    val player = Bukkit.getPlayer(args[1])
                    if (player == null) {
                        sender.error(getter["command.error.playerNotFound"])
                        return true
                    }
                    val info = PlayerManager.findInfoByPlayer(player)
                    if (info == null) {
                        sender.error(getter["command.error.playerNotFound"])
                        return true
                    }
                    if (!args[2].isDigit()) {
                        sender.error(getter["command.error.argNonDigit"])
                        return true
                    }
                    val plus = args[2].toInt() - info.currency
                    info.currency += plus

                    sender.info(getter["trade.giveCurrency.objective", player.name, plus])
                    player.info(getLang(info, "trade.giveCurrency.subjective", plus, info.currency))
                }
                "donation" -> {
                    if (sender !is Player) {
                        sender.error(getter["command.error.playerOnly"])
                        return true
                    }
                    QRUtil.sendToPlayer(File(tradeRoot, "donation.png"), sender)
                }
                "pwd" -> {
                    if (sender !is ConsoleCommandSender) {
                        sender.error(getter["command.error.perission"])
                        return true
                    }
                    if (args.size == 1) {
                        sender.sendMessage(
                            config.getString(
                                "serverPwd",
                                TextUtil.error(getter["trade.error.pwdUnset"])
                            )!!
                        )
                    } else {
                        config.set("serverPwd", args[1])
                        password = args[1]
                        saveConfig()
                        sender.info(getter["trade.pwd.setTo", args[1]])
                    }
                }
                else -> sender.error(getter["command.error.usage"])
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
        if (command.name == "trade") {
            val commands = mutableListOf("back", "center", "help", "donation")
            if (sender.isOp) commands.addAll(arrayOf("give", "set"))
            if (args.isEmpty()) {
                return commands
            } else if (args.size in 1..2 && (args.first() == "give" || args.first() == "set")) {
                val players = ArrayList<String>()
                server.onlinePlayers.forEach { players.add(it.name) }
                if (args.size > 1 && args[1].isNotEmpty()) {
                    val r = ArrayList<String>()
                    players.forEach { if (it.startsWith(args[1])) r.add(it) }
                    return r.toMutableList()
                }
                return players.toMutableList()
            } else if (args.size == 1 && args.first().isNotEmpty()) {
                val r = ArrayList<String>()
                commands.forEach {
                    if (it.startsWith(args.first())) r.add(it)
                }
                return r.toMutableList()
            }
        } else if (command.name == "bank") {
            if (sender.isOp) {
                val commands = mutableListOf("add", "remove")
                if (args.isEmpty()) {
                    return commands
                } else if (args.size == 1) {
                    return commands.filter { it.startsWith(args.first()) }.toMutableList()
                }
            }
        }
        return mutableListOf()
    }

    class TradeTerritoryInfo(val player: UUID, val id: Int) {
        val x: Int
        val z: Int
        /**
         * [fromX] is always smaller than [toX]
         */
        val fromX: Int
        val fromZ: Int
        val toX: Int
        val toZ: Int

        val center: Location
            get() {
                val dest = Location(tradeWorld, fromX + 16.0, TradeWorldGenerator.base.toDouble(), fromZ + 16.0)
                while (dest.blockY < tradeWorld.maxHeight && (dest.block.type != Material.AIR || dest.clone().add(
                        Vector(
                            0,
                            1,
                            0
                        )
                    ).block.type != Material.AIR)
                ) {
                    dest.add(Vector(0, 1, 0))
                }
                return dest
            }

        fun contains(location: Location) = location.blockX in fromX..toX && location.blockZ in fromZ..toZ

        init {
            with(Base.getUniquePair(id)) {
                this@TradeTerritoryInfo.x = first
                this@TradeTerritoryInfo.z = second
            }

            fromX = 48 * (x - 1) + 16
            fromZ = 48 * (z - 1) + 16
            toX = fromX + 32
            toZ = fromZ + 32
        }
    }

    private fun getDefaultServerTradeConfig(): YamlConfiguration {
        val r = YamlConfiguration()
        r.set("coal", 5)
        r.set("iron", 20)
        r.set("gold", 35)
        r.set("diamond", 60)
        r.set("emerald", 100)
        return r
    }

    private fun readServerTradeConfig(): YamlConfiguration {
        val file = File("plugins${File.separatorChar}trade", "serverTradeConfig.yml")
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        if (!file.exists()) {
            return getDefaultServerTradeConfig()
                .also { it.save(file) }
        }
        return YamlConfiguration.loadConfiguration(file)
    }

    override fun onEnable() {
        instance = this

        val serverTradeConfig = readServerTradeConfig()
        transMap[Material.COAL] = serverTradeConfig.getLong("coal", -1)
        transMap[Material.IRON_INGOT] = serverTradeConfig.getLong("iron", -1)
        transMap[Material.GOLD_INGOT] = serverTradeConfig.getLong("gold", -1)
        transMap[Material.DIAMOND] = serverTradeConfig.getLong("diamond", -1)
        transMap[Material.EMERALD] = serverTradeConfig.getLong("emerald", -1)
        env.set("diamondExchange", transMap[Material.DIAMOND])

        tradeWorld = WorldCreator.name("world_trade")
            .generator(getDefaultWorldGenerator("", null))
            .createWorld()!!
        tradeWorld.peace()

        QRUtil.init(this)
        password = config.getString("serverPwd", "")!!

        if (!donation.exists()) {
            logger.warning("Donation map doesn't exists! Put it at ${donation.absolutePath}")
        }

        if (server.pluginManager.isPluginEnabled("Citizens")) {
            CitizensAPI.getNPCRegistry().toList().forEach {
                if (it.data().get<Boolean?>("trade") == true)
                    it.destroy()
            }

            fun uuidFor(npc: String) = config.getString(npc, null).let {
                if (it != null) UUID.fromString(it)!!
                else {
                    val r = UUID.randomUUID()
                    config.set(npc, r.toString())
                    r!!
                }
            }

            val traderUUID = uuidFor("trader")
            val backUUID = uuidFor("back")

            npc = CitizensAPI.getNPCRegistry()
                .createNPC(EntityType.PLAYER, traderUUID, 0, EveryThing.traderInventoryName).apply {
                    spawn(Location(tradeWorld, 7.5, TradeWorldGenerator.base + 2.toDouble(), 4.toDouble()))
                    addTrait(Equipment().apply {
                        equipment[0] = ItemStack(Material.EMERALD)
                    })
                    data()["trade"] = true
                    data().saveTo(MemoryDataKey())
                }
            npcBack =
                CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, backUUID, 1, EveryThing.backNPCName).apply {
                    spawn(Location(tradeWorld, 8.5, TradeWorldGenerator.base + 2.0, 4.0))
                    data()["trade"] = true
                    data().saveTo(MemoryDataKey())
                }

            OfflineInfo.forEach {
                try {
                    it.territoryID.let { id -> territoryMap.add(TradeTerritoryInfo(it.uuid!!, id)) }
                } catch (e: Exception) {
                    logger.warning("Error while loading territory for ${it.name}")
                    e.printStackTrace()
                }
            }

            try {
                TradeManager.loadFromFile(File(tradeRoot, "tradeInfos.json"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        server.pluginManager.registerEvents(EveryThing, this)
    }

    override fun getDefaultWorldGenerator(worldName: String, id: String?): ChunkGenerator {
        return TradeWorldGenerator()
    }

    override fun onDisable() {
        TradeManager.saveToFile(File(tradeRoot, "tradeInfos.json"))
        NPCItemInventory.npcList.forEach { it.destroy() }
        CitizensAPI.getNPCRegistry().toList().forEach {
            if (it.data().get<Boolean?>("trade") == true)
                it.destroy()
            it.data()
        }
    }
}