package com.zhufu.opencraft

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.zhufu.opencraft.Base.Extend.isDigit
import com.zhufu.opencraft.Base.Extend.toPrettyString
import com.zhufu.opencraft.Base.tradeWorld
import com.zhufu.opencraft.Game.env
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.inventory.NPCExistence
import com.zhufu.opencraft.inventory.NPCItemInventory
import com.zhufu.opencraft.inventory.TradeValidateInventory
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toErrorMessage
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.util.MemoryDataKey
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.WorldCreator
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class CurrencySystem : JavaPlugin() {
    companion object {
        lateinit var npc: NPC
        lateinit var npcBack: NPC
        val transMap = HashMap<Material, Long>()
        val tradeRoot: File get() = Paths.get("plugins", "trade").toFile()
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
                        val json = JsonParser.parseString(input.readLine()).asJsonObject
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
                    val info = player.info()
                    if (info == null) {
                        player.error(getter["player.error.unknown"])
                        return true
                    }
                    val t = TradeTerritoryInfo(info)
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
                .createNPC(EntityType.WANDERING_TRADER, traderUUID, 0, MainHandle.traderInventoryName).apply {
                    data()["trade"] = true
                    data().saveTo(MemoryDataKey())
                    spawn(Location(tradeWorld, 7.5, TradeWorldGenerator.base + 2.toDouble(), 4.toDouble()))
                }
            npcBack =
                CitizensAPI.getNPCRegistry()
                    .createNPC(EntityType.ARMOR_STAND, backUUID, 1, MainHandle.backNPCName.content())
                    .apply {
                        data()["trade"] = true
                        data().saveTo(MemoryDataKey())
                        spawn(Location(tradeWorld, 8.5, TradeWorldGenerator.base + 2.0, 4.0))
                    }
        } catch (e: Exception) {
            logger.warning("Failed to spawn trader NPCs: ${e.message}")
        }

        NPCExistence.setProducer { t, l ->
            TradeValidateInventory(t, l)
        }
        try {
            TradeManager.loadFromFile(File(tradeRoot, "tradeInfos.json"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        MainHandle.init(this)
        QRUtil.init(this)
        BankManager.init(this)
        AdvertisementHandler.init(this)
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
        AdvertisementHandler.close()
    }
}