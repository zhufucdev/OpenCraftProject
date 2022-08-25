package com.zhufu.opencraft

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.zhufu.opencraft.Base.Extend.isDigit
import com.zhufu.opencraft.Base.Extend.toPrettyString
import com.zhufu.opencraft.Base.TutorialUtil.gmd
import com.zhufu.opencraft.Base.TutorialUtil.linearMotion
import com.zhufu.opencraft.Base.TutorialUtil.tplock
import com.zhufu.opencraft.Base.tradeWorld
import com.zhufu.opencraft.Game.env
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.data.OfflineInfo
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.inventory.NPCExistence
import com.zhufu.opencraft.inventory.NPCItemInventory
import com.zhufu.opencraft.inventory.TradeValidateInventory
import com.zhufu.opencraft.inventory.TraderInventory
import com.zhufu.opencraft.util.*
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.util.MemoryDataKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import java.io.File
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

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

        private fun showTitle(
            player: Player,
            getter: Language.LangGetter,
            titleCode: String,
            subtitleCode: String,
            showTime: Int,
            instant: Boolean = false
        ) {
            val title =
                Title.title(
                    getter[titleCode].toErrorMessage(),
                    Component.text(getter[subtitleCode], NamedTextColor.AQUA),
                    if (!instant)
                        Title.Times.times(
                            Duration.ofMillis(250),
                            Duration.ofSeconds(5),
                            Duration.ofMillis(250)
                        )
                    else
                        Title.Times.times(
                            Duration.ZERO,
                            Duration.ofSeconds(5),
                            Duration.ZERO
                        )
                )
            player.showTitle(title)
            Thread.sleep(showTime * 1000L + 50)
        }

        fun showTutorial(player: Player) {
            Bukkit.getScheduler().runTaskAsynchronously(Base.pluginCore, Runnable {
                val info = PlayerManager.findInfoByPlayer(player)
                if (info == null) {
                    player.error(Language.getDefault("player.error.unknown"))
                    return@Runnable
                }
                info.status = Info.GameStatus.InTutorial
                val getter = Language[info]
                player.gmd(GameMode.SPECTATOR)
                player.tplock(
                    tradeWorld.spawnLocation
                        .add(Vector(0, 30, 0))
                        .setDirection(Vector(0, -90, 0)),
                    5 * 20
                )
                showTitle(
                    player,
                    getter,
                    "trade.tutorial.1.title",
                    "trade.tutorial.1.subtitle",
                    5
                )

                val l1 = Location(tradeWorld, 7.5, 62.0, 6.5)
                    .setDirection(Vector(0, 0, -90))
                player.tplock(l1, 7 * 20)
                showTitle(
                    player,
                    getter,
                    "trade.tutorial.2.title",
                    "trade.tutorial.2.subtitle",
                    7
                )


                val l2 = territoryMap.firstOrNull { it.player == player.uniqueId }
                if (l2 == null)
                    player.sendMessage(getter["trade.error.chunkNotFound"].toErrorMessage())
                else {
                    val center = l2.center
                    val location2 =
                        Location(tradeWorld, center.x.toDouble(), tradeWorld.spawnLocation.y + 30, center.z.toDouble())
                            .setDirection(Vector(0, -90, 0))
                    player.tplock(location2, 7 * 20)
                    showTitle(
                        player,
                        getter,
                        "trade.tutorial.3.title",
                        "trade.tutorial.3.subtitle",
                        3
                    )
                    showTitle(
                        player,
                        getter,
                        "trade.tutorial.3.title",
                        "trade.tutorial.4.subtitle",
                        4,
                        true
                    )
                }
                val l3Top = tradeWorld.spawnLocation.clone()
                    .add(Vector(0, 30, 0))
                    .setDirection(Vector(0, -90, 0))
                val l3Bottom = l3Top.clone().add(Vector(0.0, -15.0, 0.0))
                val scheduler = Bukkit.getScheduler()
                scheduler.runTask(instance) { _ ->
                    player.teleport(l3Top)
                }
                player.linearMotion(l3Bottom, 13 * 20)
                showTitle(
                    player,
                    getter,
                    "trade.tutorial.5.title",
                    "trade.tutorial.5.subtitle",
                    4
                )

                showTitle(
                    player,
                    getter,
                    "trade.tutorial.5.title",
                    "trade.tutorial.6.subtitle",
                    4,
                    true
                )
                showTitle(
                    player,
                    getter,
                    "trade.tutorial.5.title",
                    "trade.tutorial.7.subtitle",
                    6,
                    true
                )

                player.linearMotion(tradeWorld.spawnLocation.setDirection(Vector(1, 0, 0)), 75)
                Thread.sleep(4000L)

                player.gmd(GameMode.ADVENTURE)
                player.showTitle(
                    Title.title(
                        getter["tutorial.begin"].toSuccessMessage(),
                        Component.text(""),
                        Title.Times.times(
                            Duration.ofMillis(150),
                            Duration.ofSeconds(2),
                            Duration.ofSeconds(1)
                        )
                    )
                )


                PlayerManager.findInfoByPlayer(player)
                    ?.also {
                        it.status = Info.GameStatus.Surviving
                        it.isTradeTutorialShown = true
                    }
                    ?: player.sendMessage(Language.getDefault("player.error.unknown").toWarnMessage())
            })
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "trade") {
            if (sender !is Player && args.size < 3 && args.first() != "pwd") {
                sender.sendMessage(getLang(sender, "command.error.playerOnly").toErrorMessage())
                return true
            }
            if (args.isEmpty()) {
                sender.sendMessage(getLang(sender, "command.error.usage").toErrorMessage())
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
                        sender.sendMessage(getter["trade.error.chunkNotFound"].toErrorMessage())
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
        } else if (command.name == "bank") {
            val getter = sender.getter()
            if (args.isEmpty()) {
                sender.info(getter["bank.found.1"])
                if (!BankManager.isEmpty()) {
                    BankManager.forEach {
                        sender.sendMessage("${it.first} @ ${it.second.toPrettyString()}")
                    }
                    if (sender is Player) {
                        val nearest = BankManager.bankNearest(sender.location)
                        if (nearest != null) sender.info(getter["bank.found.2", nearest.toPrettyString()])
                        else sender.error(getter["bank.found.none"])
                    } else {
                        sender.error(getter["command.error.playerOnly"])
                    }
                } else {
                    sender.error(getter["bank.found.none"])
                }
            } else if (sender is Player) {
                if (sender.isOp) {
                    when (args.first()) {
                        "add" -> {
                            if (args.size < 2) {
                                BankManager.createBanker(sender.location)
                                sender.success(getter["bank.createdBanker"])
                            } else {
                                val name = args[1]
                                if (!name.contains(':')) {
                                    val l = sender.location.toBlockLocation()
                                    BankManager.createBank(l, name)
                                    sender.success(getter["bank.created", name, l.toPrettyString()])
                                } else {
                                    sender.error(getter["bank.error.illegalName", name])
                                }
                            }
                        }

                        "remove" -> {
                            if (args.size < 2) {
                                val success = BankManager.removeBanker(near = sender.location)
                                if (success) {
                                    sender.success(getter["bank.removedBanker"])
                                } else {
                                    sender.error(getter["bank.error.noBankersNearby"])
                                }
                            } else {
                                val success = BankManager.removeBank(args[1])
                                if (success) {
                                    sender.success(getter["bank.removed", args[1]])
                                } else {
                                    sender.error(getter["bank.found.none"])
                                }
                            }
                        }

                    }
                } else {
                    sender.error(getter["command.error.permission"])
                }
            } else {
                sender.error(getter["command.error.playerOnly"])
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
            val commands = mutableListOf("back", "center", "donation")
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
        BankManager.init(this)
        password = config.getString("serverPwd", "")!!

        if (!donation.exists()) {
            logger.warning("Donation map doesn't exists! Put it at ${donation.absolutePath}")
        }

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

        try {
            npc = CitizensAPI.getNPCRegistry()
                .createNPC(EntityType.WANDERING_TRADER, traderUUID, 0, EveryThing.traderInventoryName).apply {
                    data()["trade"] = true
                    data().saveTo(MemoryDataKey())
                    spawn(Location(tradeWorld, 7.5, TradeWorldGenerator.base + 2.toDouble(), 4.toDouble()))
                }
            npcBack =
                CitizensAPI.getNPCRegistry()
                    .createNPC(EntityType.ARMOR_STAND, backUUID, 1, EveryThing.backNPCName.content())
                    .apply {
                        data()["trade"] = true
                        data().saveTo(MemoryDataKey())
                        spawn(Location(tradeWorld, 8.5, TradeWorldGenerator.base + 2.0, 4.0))
                    }
        } catch (e: Exception) {
            logger.warning("Failed to spawn trader NPCs: ${e.message}")
        }

        OfflineInfo.forEach {
            try {
                it.territoryID.let { id -> territoryMap.add(TradeTerritoryInfo(it.uuid!!, id)) }
            } catch (e: Exception) {
                logger.warning("Error while loading territory for ${it.name}")
                e.printStackTrace()
            }
        }

        NPCExistence.setProducer { t, l ->
            TradeValidateInventory(t, l)
        }
        try {
            TradeManager.loadFromFile(File(tradeRoot, "tradeInfos.json"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        server.pluginManager.registerEvents(EveryThing, this)

    }

    override fun getDefaultWorldGenerator(worldName: String, id: String?): ChunkGenerator {
        return TradeWorldGenerator()
    }

    override fun onDisable() {
        TradeManager.saveToFile(File(tradeRoot, "tradeInfos.json"))
        BankManager.onClose()
        NPCItemInventory.npcList.forEach { it.destroy() }
        CitizensAPI.getNPCRegistry().toList().forEach {
            if (it.data().get<Boolean?>("trade") == true)
                it.destroy()
        }
    }
}