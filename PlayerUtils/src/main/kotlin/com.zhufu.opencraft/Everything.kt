package com.zhufu.opencraft

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.zhufu.opencraft.Base.Extend.appendToJson
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.ui.MenuInterface
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toInfoMessage
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import java.io.File
import java.io.StringReader
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object Everything : Listener {
    var mPlugin: Plugin? = null
    val cubes = ArrayList<Cube>()
    val file: File
        get() = File(mPlugin!!.dataFolder, "miniGames.json").also {
            if (!it.parentFile.exists()) it.parentFile.mkdirs()
            if (!it.exists()) it.createNewFile()
        }

    fun init(plugin: Plugin) {
        mPlugin = plugin
        plugin.server.pluginManager.registerEvents(this, plugin)

        val reader = JsonReader(file.reader())
        try {
            reader.beginArray()
            while (reader.hasNext()) {
                cubes.add(Cube.fromJson(JsonParser().parse(reader).asJsonObject) ?: continue)
            }
            reader.endArray()
        } catch (e: Exception) {
            print("[PlayerUtil] Unable to load configurations: ${e.message}: ${e.cause}")
            e.printStackTrace()
        }
    }

    class Cube(val from: Location, val to: Location) {
        var type = "DTWB"
        val data = YamlConfiguration()
        var savedData = JsonObject()

        fun contains(location: Location): Boolean {
            fun validate(a: Int, b: Int, x: Int) =
                if (a < b) x in a..b else x in b..a

            return validate(from.blockX, to.blockX, location.blockX)
                    && validate(from.blockY, to.blockY, location.blockY)
                    && validate(from.blockZ, to.blockZ, location.blockZ)
        }

        fun fill(world: World, material: Material) {
            fun doZ(x: Int, y: Int) {
                if (from.blockZ < to.blockZ) {
                    for (z in from.blockZ..to.blockZ) {
                        //world.getBlockAt(x,y,z).setTypeIdAndData(material.itemType.uuid,material.data,true)
                        world.getBlockAt(x, y, z).also {
                            it.setType(material, true)
                        }
                    }
                } else {
                    for (z in to.blockZ..from.blockZ) {
                        world.getBlockAt(x, y, z).also {
                            it.setType(material, true)
                        }
                    }
                }
            }

            fun doY(x: Int) {
                if (from.blockY < to.blockY) {
                    for (y in from.blockY..to.blockY) {
                        doZ(x, y)
                    }
                } else {
                    for (y in to.blockY..from.blockY) {
                        doZ(x, y)
                    }
                }
            }
            if (from.blockX < to.blockX) {
                for (x in from.blockX..to.blockX) {
                    doY(x)
                }
            } else {
                for (x in to.blockX..from.blockX) {
                    doY(x)
                }
            }
        }

        fun appendToJson(writer: JsonWriter) {
            writer.beginObject()
                .name("from")
            from.appendToJson(writer)
            writer.name("to")
            to.appendToJson(writer)
            writer
                .name("type").value(type)
                .name("data").jsonValue(savedData.toString())
                .endObject()
        }

        companion object {
            fun fromJson(obj: JsonObject): Cube? {
                var from: Location? = null
                var to: Location? = null
                if (obj.has("from")) {
                    from = Base.Extend.fromJsonToLocation(JsonReader(StringReader(obj["from"].asJsonObject.toString())))
                }
                if (obj.has("to")) {
                    to = Base.Extend.fromJsonToLocation(JsonReader(StringReader(obj["to"].asJsonObject.toString())))
                }
                if (from == null || to == null)
                    return null
                val r = Cube(from, to)
                if (obj.has("type"))
                    r.type = obj["type"].asString
                if (obj.has("data")) {
                    r.savedData = obj["data"].asJsonObject
                }
                return r
            }
        }

        override fun equals(other: Any?): Boolean {
            return other is Cube && other.from == this.from && other.to == this.to
        }

        override fun hashCode(): Int {
            var result = from.hashCode()
            result = 31 * result + to.hashCode()
            return result
        }
    }

    fun onServerClose() {
        val writer =
            GsonBuilder()
                .setPrettyPrinting()
                .create()
                .newJsonWriter(file.writer())
        writer.beginArray()
        cubes.forEach {
            it.appendToJson(writer)
        }
        writer.endArray()
            .flush()
    }

    fun createDTWB(from: Location, to: Location) {
        val element = Cube(from, to)
        if (!cubes.contains(element)) {
            print("Creating DTWB")
            cubes.add(element)
            element.fill(from.world!!, Material.WHITE_CONCRETE)
        }
    }

    fun index(location: Location) = cubes.firstOrNull { it.contains(location) }
    fun startDTWB(game: Cube) {
        print("Starting DTWB")
        val yPair = (game.from.blockY to game.to.blockY).toList()
        for (y in yPair.min()..yPair.max()) {
            spawnDTWBat(y, game)
        }
        game.apply {
            data.set("isStarted", true)
            data.set("lastClick", null)
            data.set("speed", null)
        }
    }

    private fun spawnDTWBat(y: Int, game: Cube) {
        val add: Vector
        val max: Int
        if (game.to.blockX == game.from.blockX) {
            add = Vector(0, 0, 1)
            max = abs(game.from.blockZ - game.to.blockZ)
        } else {
            add = Vector(1, 0, 0)
            max = abs(game.from.blockX - game.to.blockX)
        }

        val base =
            if (game.from.blockX < game.to.blockX || game.from.blockZ < game.to.blockZ) game.from.clone() else game.to.clone()
        base.y = y.toDouble()
        var spawned = false
        for (i in 1..max) {
            if (!spawned && Base.trueByPercentages(1 / max.toFloat())) {
                base.block.setType(Material.BLACK_CONCRETE, true)
                spawned = true
            } else {
                base.block.type = Material.WHITE_CONCRETE
            }
            base.add(add)
        }
        if (!spawned) {
            base.block.setType(Material.BLACK_CONCRETE, true)
        } else {
            base.block.type = Material.WHITE_CONCRETE
        }
    }

    private fun continueDTWB(location: Location, game: Cube, player: Player) {
        val speed = game.data.getLong("speed", -1)
        val base = if (game.from.blockY < game.to.blockY) {
            game.from
        } else {
            game.to
        }

        if (location.block.type != Material.BLACK_CONCRETE || location.blockY != base.blockY) {
            game.fill(location.world!!, Material.RED_CONCRETE)
            game.data.set("isStarted", false)
            player.error("游戏结束!")
        } else if (
            speed != -1L &&
            System.currentTimeMillis() - game.data.getLong("lastClick", System.currentTimeMillis())
            > speed
        ) {
            game.fill(location.world!!, Material.RED_CONCRETE)
            game.data.set("isStarted", false)
            player.error("游戏结束: 操作已超时")
        } else {
            val maxXZ: Int
            val maxY = abs(game.from.blockY - game.to.blockY)

            val add: Vector
            if (game.from.blockX == game.to.blockX) {
                add = Vector(0, 0, 1)
                maxXZ = abs(game.from.blockZ - game.to.blockZ)
            } else {
                add = Vector(1, 0, 0)
                maxXZ = abs(game.from.blockX - game.to.blockX)
            }
            val a = base.clone()
            for (y in 1..maxY) {
                for (xOrz in 0..maxXZ) {
                    val toBeFilled = a.block
                    val toFill = a.clone().add(Vector(0, 1, 0)).block
                    toBeFilled.setType(toFill.type, true)
                    a.add(add)
                }
                a.x = base.x
                a.z = base.z
                a.add(Vector(0, 1, 0))
            }
            spawnDTWBat(a.blockY, game)

            game.data.set("lastClick", System.currentTimeMillis())
            val newSpeed = game.data.getLong("speed", 2000) - (30.0 / maxXZ).roundToLong()
            if (newSpeed >= maxXZ * 1.5)
                game.data.set("speed", newSpeed)
            player.sendActionBar("最大等待: $newSpeed".toInfoMessage())
        }
    }

    fun createTP(from: Location, to: Location, owner: String? = "op") {
        val cube = Cube(from, to)
        cube.type = "TP"
        cube.savedData.addProperty("owner", owner)
        cubes.add(cube)
    }

    fun createCRT(location: Location) {
        val base = location.blockLocation.add(Vector(0, -1, 0))
        val yaw = abs(location.yaw.roundToInt() % 360)
        val cube: Cube
        mPlugin!!.logger.info("Yaw is $yaw")
        when (yaw) {
            in 315..360, in 0 until 45 -> {
                cube = Cube(base.clone().add(Vector(1, 0, 0)), base.clone().add(Vector(-1, 0, 0)))
                cube.savedData.addProperty("yaw", 0)
            }
            in 45 until 135 -> {
                cube = Cube(base.clone().add(Vector(0, 0, 1)), base.clone().add(Vector(0, 0, -1)))
                cube.savedData.addProperty("yaw", 270)
            }
            in 135 until 225 -> {
                cube = Cube(base.clone().add(Vector(1, 0, 0)), base.clone().add(Vector(-1, 0, 0)))
                cube.savedData.addProperty("yaw", 180)
            }
            else -> {
                cube = Cube(base.clone().add(Vector(0, 0, 1)), base.clone().add(Vector(0, 0, -1)))
                cube.savedData.addProperty("yaw", 90)
            }
        }
        cube.type = "CRT"
        cube.fill(location.world!!, Material.BLUE_CONCRETE)
        cubes.add(cube)

        ChartHandler.spawnNPC(cube, Game.dailyChart)
    }

    private val clickMap = HashMap<Player, Pair<Int, Long>>()

    @EventHandler
    fun onPlayerClick(event: PlayerInteractEvent) {
        fun plus() {
            val player = event.player
            val info = player.info()
            if (info?.status != Info.GameStatus.Surviving)
                return
            if (!info.preference.playerUtilitiesGesture)
                return
            fun reset() {
                clickMap[player] = 1 to System.currentTimeMillis()
            }
            if (clickMap.containsKey(player)) {
                val value = clickMap[player]!!
                if (System.currentTimeMillis() - value.second <= 1200)
                    clickMap[player] = value.first + 1 to value.second
                else {
                    reset()
                }
            } else {
                reset()
            }
        }

        if (event.action == Action.LEFT_CLICK_BLOCK) {
            val game = index(event.clickedBlock!!.location)
            if (game?.type == "DTWB") {
                if (!game.data.getBoolean("isStarted", false)) {
                    startDTWB(game)
                } else {
                    continueDTWB(event.clickedBlock!!.location, game, event.player)
                }
            } else {
                plus()
            }
        } else if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
            plus()
        } else if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            val player = event.player
            val value = clickMap[player]
            if (value != null && value.first >= 3) {
                if (System.currentTimeMillis() - value.second <= 1200) {
                    MenuInterface(mPlugin!!, player).show(player)
                    event.isCancelled = true
                }
                clickMap.remove(player)
            }
        }
    }

    fun Location.near(b: Location): Boolean {
        val d =
            (x.toFloat() - b.x.toFloat()).pow(2) + (y.toFloat() - b.y.toFloat()).pow(2) + (z.toFloat() - b.z.toFloat()).pow(
                2
            )
        return b.world == this.world && d < 1
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        cubes.removeIf {
            if (it.type != "TP") {
                if (it.type == "CRT" && it.contains(event.block.location)) {
                    if (event.player.isOp) {
                        ChartHandler.remove(it)
                        true
                    } else {
                        event.isCancelled = true
                        false
                    }
                } else it.contains(event.block.location)
            } else false
        }
    }

    @EventHandler
    fun onBookEdit(event: PlayerEditBookEvent) {
        if (originItemMap.containsKey(event.player.uniqueId)) {
            val pair = originItemMap[event.player.uniqueId]!!
            pair.second(
                buildString {
                    for (i in 0 until event.newBookMeta.pageCount) {
                        val text = (event.newBookMeta.page(i) as TextComponent).content()
                        var index = 0
                        while (index < text.length) {
                            val c = text[index]
                            if (c != TextUtil.KEY || !text[index + 1].isDigit()) {
                                append(c)
                            } else {
                                index++
                            }
                            index++
                        }
                    }
                }
            )
            Bukkit.getScheduler().runTaskLater(mPlugin!!, { _ ->
                event.player.inventory.setItemInMainHand(pair.first)
            }, 5)
        }
    }

    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {
        if (event.itemDrop.itemStack.itemMeta!!.displayName() == event.player.info().getter()["scripting.ui.new"].toInfoMessage()
            && originItemMap.containsKey(event.player.uniqueId)
        ) {
            event.isCancelled = true

            val pair = originItemMap[event.player.uniqueId]!!
            pair.second(null)
            Bukkit.getScheduler().runTaskLater(mPlugin!!, { _ ->
                event.player.inventory.setItemInMainHand(pair.first)
            }, 5)
        }
    }

    private val originItemMap = HashMap<UUID, Pair<ItemStack, (String?) -> Unit>>()
}