package com.zhufu.opencraft

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.zhufu.opencraft.Base.TutorialUtil.gmd
import com.zhufu.opencraft.Base.TutorialUtil.tplock
import com.zhufu.opencraft.Base.TutorialUtil.linearTo
import com.zhufu.opencraft.Base.tradeWorld
import com.zhufu.opencraft.CurrencySystem.TradeTerritoryInfo.Direction.*
import com.zhufu.opencraft.inventory.TraderInventory
import com.zhufu.opencraft.Base.Extend.isDigit
import com.zhufu.opencraft.Game.env
import com.zhufu.opencraft.WorldUtil.peace
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.generator.ChunkGenerator
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import java.util.concurrent.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CurrencySystem : JavaPlugin(), PluginBase {
    companion object {
        var npc: NPC? = null
        val transMap = HashMap<Material, Long>()
        val inventoryMap = ArrayList<TraderInventory>()
        val territoryMap = ArrayList<TradeTerritoryInfo>()
        val tradeRoot = File("plugins${File.separatorChar}trade")
        val donation: File
            get() = File(tradeRoot, "donation.png")

        var server: ServerSocket? = null
        var checker: ServerSocket? = null
        var client: Socket? = null
        private val threadPool = Executors.newCachedThreadPool()
        val isServerReady: Boolean
            get() {
                val connection = FutureTask<Boolean> {
                    var result: Boolean

                    if (checker == null || checker!!.isClosed){
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
                return try { connection[2,TimeUnit.SECONDS] } catch (e: TimeoutException) { false }
            }
        var password = ""

        lateinit var mInstance: CurrencySystem

        fun showTutorial(player: Player) {
            Bukkit.getScheduler().runTaskAsynchronously(Base.TutorialUtil.mPlugin, Runnable{
                val info = PlayerManager.findInfoByPlayer(player)
                if (info == null){
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
                        5 * 1000L)
                player.sendTitle(TextUtil.error(getter["trade.tutorial.1.title"]),
                        TextUtil.getColoredText(getter["trade.tutorial.1.subtitle"], TextUtil.TextColor.AQUA, false, false),
                        7, 5 * 20, 7)
                Thread.sleep(5 * 1000L + 50)

                val l1 = Location(tradeWorld, 7.5, 62.0, 6.5)
                        .setDirection(Vector(0, 0, -90))
                player.tplock(l1, 7 * 1000L)
                player.sendTitle(TextUtil.tip(getter["trade.tutorial.2.title"]),
                        TextUtil.getColoredText(getter["trade.tutorial.2.subtitle"], TextUtil.TextColor.AQUA, false, false),
                        7, 7 * 20, 7)
                Thread.sleep(7 * 1000L + 50)

                val l2 = BlockLockManager["${player.uniqueId}_territory"]
                if (l2 == null || l2 !is BlockLockManager.BlockInfo)
                    player.sendMessage(TextUtil.error(getter["trade.error.chunkNotFound"]))
                else {
                    val center = l2.center
                    val L2 = Location(tradeWorld, center.x.toDouble(), tradeWorld.spawnLocation.y + 30, center.z.toDouble())
                            .setDirection(Vector(0, -90, 0))
                    player.tplock(L2, 7 * 1000L)
                    player.sendTitle(TextUtil.tip(getter["trade.tutorial.3.title"]),
                            TextUtil.getColoredText(getter["trade.tutorial.3.subtitle"], TextUtil.TextColor.AQUA, false, false),
                            7, 3 * 20, 0)
                    Thread.sleep(3 * 1000L)
                    player.sendTitle(TextUtil.tip(getter["trade.tutorial.3.title"]),
                            TextUtil.getColoredText(getter["trade.tutorial.4.subtitle"], TextUtil.TextColor.AQUA, false, false),
                            0, 4 * 20, 7)
                    Thread.sleep(4 * 1000L + 50)
                }
                val l3Top = tradeWorld.spawnLocation.clone()
                        .add(Vector(0, 30, 0))
                        .setDirection(Vector(0, -90, 0))
                val l3Bottom = l3Top.clone().add(Vector(0.0, -15.0, 0.0))
                player.teleport(l3Top)
                player.sendTitle(TextUtil.tip(getter["trade.tutorial.5.title"]),
                        TextUtil.getColoredText(getter["trade.tutorial.5.subtitle"], TextUtil.TextColor.AQUA, false, false), 7, 4 * 20, 0)
                player.linearTo(l3Bottom, 13 * 1000L, 20)
                Thread.sleep(4 * 1000L + 50)
                player.sendTitle(TextUtil.tip(getter["trade.tutorial.5.title"]),
                        TextUtil.getColoredText(getter["trade.tutorial.6.subtitle"], TextUtil.TextColor.AQUA, false, false), 0, 4 * 20, 0)
                Thread.sleep(4 * 1000L + 50)
                player.sendTitle(TextUtil.tip(getter["trade.tutorial.5.title"]),
                        TextUtil.getColoredText(getter["trade.tutorial.7.subtitle"], TextUtil.TextColor.AQUA, false, false), 0, 6 * 20, 7)
                Thread.sleep(6 * 1000L + 50)

                player.linearTo(tradeWorld.spawnLocation, 1500L)
                Thread.sleep(1500L)

                player.gmd(GameMode.ADVENTURE)
                player.sendTitle(TextUtil.getColoredText(getter["tutorial.begin"], TextUtil.TextColor.RED, true, false), "", 7, 60, 7)

                PlayerManager.findInfoByPlayer(player)
                        ?.also { it.status = Info.GameStatus.Surviving }
                        ?.tag?.set("isTradeTutorialShown", true) ?: player.sendMessage(TextUtil.warn(Language.getDefault("player.error.unknown")))
            })
        }
    }

    private fun showHelp(): String = File(dataFolder, "help.txt")
            .also { if (!it.parentFile.exists()) it.parentFile.mkdirs() }
            .readText()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "trade") {
            if (sender !is Player && args.size < 3 && args.first() != "pwd") {
                sender.sendMessage(TextUtil.error(getLang(sender,"command.error.playerOnly")))
                return true
            }
            if (args.isEmpty()) {
                sender.sendMessage(TextUtil.error(getLang(sender,"command.error.usage")))
                return true
            }

            val getter = getLangGetter(if (sender is Player) PlayerManager.findInfoByPlayer(sender.uniqueId) else null)
            when (args.first()) {
                "back" -> {
                    val player = sender as Player
                    val t = territoryMap.firstOrNull { it.player == player.uniqueId }
                    if (t == null) {
                        sender.sendMessage(TextUtil.error(getter["trade.error.chunkNotFound"]))
                        return true
                    }
                    val dest = t.toBlockLocker().center.toLocation(tradeWorld, TradeWorldGenerator.base + 2.0)
                    while (dest.blockY < 255 && (dest.block.type != Material.AIR || dest.clone().add(Vector(0,1,0)).block.type != Material.AIR)){
                        dest.add(Vector(0,1,0))
                    }
                    val event = PlayerTeleportedEvent(sender, sender.location, dest)
                    server.pluginManager.callEvent(event)
                    if (!event.isCancelled) {
                        sender.teleport(dest)
                    }
                }
                "center" -> {
                    val player = sender as Player
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
                    info.currency += args[2].toInt()

                    sender.info(getter["trade.giveCurrency.objective",player.name,args[2]])
                    player.info(getLang(info,"trade.giveCurrency.subjective",args[2],info.currency))
                }
                "set" -> {
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

                    sender.info(getter["trade.giveCurrency.objective",player.name,plus])
                    player.info(getLang(info,"trade.giveCurrency.subjective",plus,info.currency))
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
                        sender!!.error(getter["command.error.perission"])
                        return true
                    }
                    if (args.size == 1) {
                        sender.sendMessage(config.getString("serverPwd", TextUtil.error(getter["trade.error.pwdUnset"]))!!)
                    } else {
                        config.set("serverPwd", args[1])
                        CurrencySystem.password = args[1]
                        saveConfig()
                        sender.info(getter["trade.pwd.setTo",args[1]])
                    }
                }
                else -> sender!!.error(getter["command.error.usage"])
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        if (command.name == "trade") {
            val commands = mutableListOf("back", "center", "help", "donation")
            if (sender.isOp) commands.addAll(arrayOf("give", "set"))
            if (args.size in 1..2 && (args.first() == "give" || args.first() == "set")) {
                val players = ArrayList<String>()
                server.onlinePlayers.forEach { players.add(it.name) }
                if (args[1].isNotEmpty()) {
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
            return commands
        }
        return mutableListOf()
    }

    class TradeTerritoryInfo(val player: UUID, val id: Int) {
        var location = BlockLockManager.XZ(0, 0)

        enum class Direction(val i: Int) {
            UP(0), LEFT(1), DOWN(2), RIGHT(3);

            fun next(): Direction {
                return if (i + 1 > 3)
                    values().first()
                else values()[i + 1]
            }
        }

        fun getChunkLocation(): BlockLockManager.XZ {
            var limit = 1
            var direction = UP
            val r = BlockLockManager.XZ(0, 0)
            var t = 0
            var j = 0
            var k = 3
            var correct = 0
            while (true) {
                t++
                when (direction) {
                    LEFT -> r.x++
                    RIGHT -> r.x--
                    UP -> r.z++
                    DOWN -> r.z--
                }
                if (r.x % 3 != 0 && r.z % 3 != 0) {
                    correct++
                    if (correct > id)
                        break
                }
                j++
                if (j == k) {
                    k++
                    limit++
                    j = 0
                }
                if (t >= limit) {
                    t = 0
                    direction = direction.next()
                }
            }
            return r
        }

        fun toBlockLocker(name: String = ""): BlockLockManager.BlockInfo {
            val from: BlockLockManager.XZ
            val to: BlockLockManager.XZ
            if (location.x > 0 && location.z > 0) {
                from = BlockLockManager.XZ(location.x * 48 - 16, location.z * 48 - 16)
                to = from.clone()
                        .also {
                            it.x -= 32
                            it.z -= 32
                        }
            } else if (location.x > 0 && location.z < 0) {
                from = BlockLockManager.XZ(location.x * 48 - 16, location.z * 48)
                to = from.clone()
                        .also {
                            it.x -= 32
                            it.z += 32
                        }
            } else if (location.x < 0 && location.z > 0) {
                from = BlockLockManager.XZ(location.x * 48, location.z * 48 - 16)
                to = from.clone()
                        .also {
                            it.x += 32
                            it.z -= 32
                        }
            } else {
                from = BlockLockManager.XZ(location.x * 48, location.z * 48)
                to = from.clone()
                        .also {
                            it.x += 32
                            it.z += 32
                        }
            }
            var t: Int
            t = from.x * -1
            from.x = from.z * -1
            from.z = t
            t = to.x * -1
            to.x = to.z * -1
            to.z = t

            return BlockLockManager.BlockInfo(
                    from = from,
                    to = to,
                    world = "world_trade",
                    name = if (!name.isEmpty()) "${name}_territory" else "NULL"
            ).also {
                it.owner = name
            }
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
        mInstance = this

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
        CurrencySystem.password = config.getString("serverPwd", "")!!

        if (!donation.exists()) {
            logger.warning("Donation map doesn't exist! Put it at ${donation.absolutePath}")
        }

        Bukkit.getScheduler().runTaskLater(this, { _ ->
            if (server.pluginManager.isPluginEnabled("Citizens")) {
                npc = CitizensAPI.getNPCRegistry()
                        .createNPC(EntityType.PLAYER, EveryThing.traderInventoryName)
                npc!!.spawn(Location(tradeWorld, 7.5, TradeWorldGenerator.base + 2.toDouble(), 4.toDouble()))
                val entity = npc!!.entity as Player
                entity.inventory.setItemInMainHand(ItemStack(Material.EMERALD))
            }
            server.pluginManager.registerEvents(EveryThing, this)

            OfflineInfo.forEach {
                val conf = it.tag
                try {
                    if (conf.isSet("territoryID")) {
                        territoryMap.add(
                                TradeTerritoryInfo(it.uuid!!, conf.getInt("territoryID"))
                                        .also { info ->
                                            if (conf.isSet("territoryLocation"))
                                                info.location = conf["territoryLocation"] as BlockLockManager.XZ
                                            else {
                                                info.location = info.getChunkLocation()
                                            }
                                        }
                        )
                    }
                } catch (e: Exception) {
                    logger.warning("Error while loading territory ${file.name}")
                    logger.warning("${e::class.simpleName}: ${e.localizedMessage}, ${e.cause}")
                }
            }

            try {
                TradeManager.loadFromFile(File(tradeRoot, "tradeInfos.json"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 40)
    }

    override fun getDefaultWorldGenerator(worldName: String, id: String?): ChunkGenerator? {
        return TradeWorldGenerator()
    }

    override fun onDisable() {
        npc?.destroy()
        TradeManager.saveToFile(File(tradeRoot, "tradeInfos.json"))
        tradeWorld.entities.forEach {
            if (it is Item)
                it.remove()
        }
        NPCItemInventory.npcList.forEach { it.destroy() }
    }
}