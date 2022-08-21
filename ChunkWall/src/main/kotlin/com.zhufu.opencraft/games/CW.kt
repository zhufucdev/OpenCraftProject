package com.zhufu.opencraft.games

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.FlatRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.world.block.BaseBlock
import com.zhufu.opencraft.*
import com.zhufu.opencraft.chunkgenerator.VoidGenerator
import com.zhufu.opencraft.events.PlayerQuitGameEvent
import com.zhufu.opencraft.util.*
import net.kyori.adventure.title.Title.Times
import net.kyori.adventure.title.Title.title
import org.bukkit.*
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import parsii.eval.Parser
import parsii.eval.Scope
import java.io.File
import java.time.Duration
import java.util.ArrayList
import javax.naming.OperationNotSupportedException
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

class CW : MiniGame() {
    companion object {

        fun getLocationByTeam(team: Team, world: World) = when (team) {
            Team.RED -> Location(world, (-8).toDouble(), 129.toDouble(), (-8).toDouble())
            Team.BLUE -> Location(world, (-8).toDouble(), 129.toDouble(), 8.toDouble())
            Team.GREEN -> Location(world, 8.toDouble(), 129.toDouble(), 8.toDouble())
            Team.YELLOW -> Location(world, 8.toDouble(), 129.toDouble(), (-8).toDouble())
            else -> Location(world, 0.toDouble(), 0.toDouble(), 0.toDouble())
        }

        lateinit var originWorld: World
        private fun createNewGameWorld(id: Int, plugin: JavaPlugin): World {
            val w = Bukkit.createWorld(
                WorldCreator("game_cw_$id")
                    .type(WorldType.FLAT)
                    .generator(VoidGenerator(128))
            )
            val wb = w!!.worldBorder
            wb.setCenter(0.toDouble(), 0.toDouble())
            wb.size = 16 * 2.toDouble()
            wb.warningDistance = 1

            val chunks = listOf(w.getChunkAt(0, 0), w.getChunkAt(-1, -1), w.getChunkAt(0, -1), w.getChunkAt(-1, 0))
            val biomeAlreadyUsed = ArrayList<Biome>()
            val editSession = WorldEdit.getInstance().newEditSession(BukkitWorld(w))
            chunks.map { cnk ->
                println("Preparing blocks for chunk ${cnk.x} ${cnk.z}")
                //Calculations
                var r = Base.getRandomLocation(originWorld, 100000)
                var b = originWorld.getBiome(r.blockX, r.toHighestLocation().blockY, r.blockZ)
                var times = 0
                measureTimeMillis {
                    while (true) {
                        if (!biomeAlreadyUsed.contains(b) && b != Biome.OCEAN) {
                            biomeAlreadyUsed.add(b)
                            break
                        }
                        r.chunk.unload(false)
                        r = Base.getRandomLocation(originWorld, 10000)
                        b = originWorld.getBiome(r.blockX, r.toHighestLocation().blockY, r.blockZ)
                        if (times >= 4000) {
                            throw WorldSpawnTimeOutException(
                                w,
                                "Unable to find suitable biome."
                            )
                        }
                        times++
                    }
                }.let {
                    Bukkit.getLogger().info("Biome looking-up took ${it}ms")
                }
                val chunk = r.chunk
                measureTimeMillis {
                    chunk.load(true)
                    cnk.load()
                }.let {
                    Bukkit.getLogger().info("Chunk loader took ${it}ms")
                }

                val origin = chunk.getBlock(0, originWorld.minHeight, 0).location
                val farthest = chunk.getBlock(15, 128, 15).location
                val copyFrom = CuboidRegion(
                    BukkitWorld(originWorld),
                    BlockVector3.at(origin.blockX, origin.blockY, origin.blockZ),
                    BlockVector3.at(farthest.blockX, farthest.blockY, farthest.blockZ)
                )
                val targetOrigin = cnk.getBlock(0, w.minHeight, 0).location
                val copyTo = BlockVector3.at(targetOrigin.blockX, targetOrigin.blockY, targetOrigin.blockZ)
                measureTimeMillis {
                    Operations.complete(
                        ForwardExtentCopy(editSession, copyFrom, BukkitWorld(w), copyTo).apply {
                            isCopyingBiomes = true
                            isCopyingEntities = false
                        }
                    )
                }.let {
                    Bukkit.getLogger().info("Chunk copying took ${it}ms")
                }
            }

            Bukkit.getLogger().info("Using biomes: ${biomeAlreadyUsed.joinToString()}")

            Bukkit.getLogger().info("Filling barriers")
            val barrierRegions = listOf(
                CuboidRegion(
                    BlockVector3.at(-16, 128, 16),
                    BlockVector3.at(16, 128, -16)
                ),
                CuboidRegion(
                    BlockVector3.at(-16, w.maxHeight, 16),
                    BlockVector3.at(16, w.maxHeight, -16)
                )
            )
            val barrierPattern = BukkitAdapter.adapt(Material.BARRIER.createBlockData())
            barrierRegions.forEach {
                editSession.setBlocks(it, barrierPattern)
            }
            editSession.close()

            /**
             * Change TIME & DIFFICULTY, ETC...
             */
            w.time = 2000
            w.difficulty = Difficulty.PEACEFUL
            Bukkit.getScheduler().runTaskLater(plugin, { _ ->
                w.difficulty = Difficulty.NORMAL
            }, 50)
            return w
        }

        class WorldSpawnTimeOutException(world: World, msg: String) : Exception(msg)
    }

    lateinit var world: World
    var winner = Team.NONE

    override fun initGame(id: Int, player: Player?, plugin: JavaPlugin) {
        super.initGame(id, player, plugin)

        player?.sendMessage("正在创建新的房间，请稍后".toInfoMessage())
        try {
            world = createNewGameWorld(id, plugin)
        } catch (e: WorldSpawnTimeOutException) {
            player?.sendMessage("创建超时".toErrorMessage())
            player?.sendMessage(*TextUtil.printException(e))
        }
    }

    override fun getGameName(): String = "CW"

    override fun getWaitingRoom(player: Player): Location = Location(world, 0.toDouble(), 129.toDouble(), 0.toDouble())

    override fun getTeams(): Array<Team> = arrayOf(Team.BLUE, Team.RED, Team.GREEN, Team.YELLOW)

    override fun getPlayerCount(): Int = 2

    override fun getMaxPlayerCount(): Int = 20

    override fun getDefaultGameMode(): GameMode = GameMode.ADVENTURE

    override fun getGameRulers(): Array<GameRuler> = arrayOf(gameRuler, pvpRuler, resultRuler)

    private val ores = listOf(Material.IRON_ORE, Material.COAL_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE)
    private fun getOrePercentageByYRay(ore: Material, y: Int): Float {
        val code = ores.indexOf(ore)
        fun getCodeByOre(ore: Material) = when (ore) {
            Material.COAL_ORE -> "coal"
            Material.IRON_ORE -> "iron"
            Material.GOLD_ORE -> "gold"
            Material.DIAMOND_ORE -> "diamond"
            else -> ""
        }

        val ranges = ArrayList<String>()
        val map =
            YamlConfiguration.loadConfiguration(File("plugins/ChunkWall/config.yml")).getMapList(getCodeByOre(ore))
        map.first().keys.forEach {
            ranges.add(it as String)
        }
        var calc = ""
        for (it in ranges) {
            val separator = it.indexOf('-')
                .also { if (it == -1) throw OperationNotSupportedException("Unable to find '-' mark in a ore-percentage line for $code at \"$it\"") }
            val a = it.substring(0, separator).toInt()
            val b = it.substring(separator + 1).toInt()

            if (y in a..b) {
                calc = map.first()[it] as String
                break
            }
        }
        if (calc.isEmpty()) {
            return 0f
        }

        val scope = Scope()
        val valY = scope.getVariable("y")
        valY.value = y.toDouble()
        val expr = Parser.parse(calc, scope)
        return expr.evaluate().toFloat()
    }

    private fun spawnOreAt(y: Int): Material {
        ores.forEach {
            if (Base.trueByPercentages(getOrePercentageByYRay(it, y))) {
                return it
            }
        }
        return Material.STONE
    }

    private val gameRuler = object : GameRuler() {
        override fun getAllowBlockBreaking(): Boolean = true
        override fun getAllowBlockPlacing(): Boolean = true

        override fun getAllowPVP(): Boolean = false

        override fun onEnable() {
            players.forEach {
                it.player.isInvulnerable = true
                it.player.showTitle(
                    title(
                        "游戏开始".toWarnMessage(),
                        "你有十五分钟时间".toTipMessage(),
                        titleDurationLong
                    )
                )

                it.player.inventory.addItem(ItemStack(Material.OAK_LOG, 12))
            }
        }

        override fun onTimeChanged(i: Long, limit: Long) {
            players.forEach {
                val scoreboard = getScoreboard(it.player, "区块战墙", teamScores!!, i, limit, "生存")
                it.player.scoreboard = scoreboard
            }
        }

        override fun getTimeLimit(): Long = 15 * 60 * 1000L

        override fun getWorld(): World = world

        override fun getPlayerLocation(info: Gaming.PlayerGamingInfo): Location =
            getLocationByTeam(info.team, world)

        override fun getGameMode(): GameMode = GameMode.SURVIVAL

        @EventHandler
        fun onPlayerMove(event: PlayerMoveEvent) {
            if (!validatePlayer(event.player))
                return
            val to = event.to.clone().add(Vector(0, -1, 0))
            val info = players.findInfoByPlayer(event.player) ?: return
            if (to.world!!.getBlockAt(to).type != Material.AIR && !info.tag.getBoolean("unprotected", false)) {
                info.tag.set("unprotected", true)
                Bukkit.getScheduler().runTaskLater(plugin, { t ->
                    val player = event.player
                    player.showTitle(
                        title(
                            "您已失去出生保护".toInfoMessage(),
                            "同时，您的重生点已设置为(${to.x},${to.y},${to.z})".toComponent(),
                            titleDurationShort
                        )
                    )
                    player.isInvulnerable = false
                    info.tag.set("respawn", to)
                }, 80L)
            }
        }

        @EventHandler
        fun onPlayerDeath(event: PlayerDeathEvent) {
            if (!validatePlayer(event.entity.player))
                return
            event.keepInventory = false
        }

        @EventHandler
        fun onPlayerRespawn(event: PlayerRespawnEvent) {
            if (!validatePlayer(event.player))
                return
            event.respawnLocation = ((players.findInfoByPlayer(event.player) ?: return).tag["respawn"] as Location)
        }

        val stoneExcepted = ArrayList<Location>()

        @EventHandler
        fun onBlockPlaced(event: BlockPlaceEvent) {
            if (!validatePlayer(event.player))
                return
            if (event.blockPlaced.type == Material.STONE) {
                stoneExcepted.add(event.blockPlaced.location)
            }
        }

        @EventHandler
        fun onBlockBroken(event: BlockBreakEvent) {
            if (!validatePlayer(event.player))
                return
            val location = event.block.location
            if (event.block.type == Material.STONE) {
                val index = stoneExcepted.indexOf(location)
                if (index != -1) {
                    stoneExcepted.removeAt(index)
                    return
                } else {
                    val spawn = spawnOreAt(location.blockY)
                    if (spawn != Material.STONE) {
                        event.isDropItems = false
                        world.dropItem(
                            location,
                            when (spawn) {
                                Material.COAL_ORE -> ItemStack(Material.COAL)
                                Material.IRON_ORE -> ItemStack(Material.IRON_INGOT)
                                Material.GOLD_ORE -> ItemStack(Material.GOLD_INGOT)
                                Material.DIAMOND_ORE -> ItemStack(Material.DIAMOND)
                                else -> return
                            }
                        )

                        val x = location.blockX
                        val y = location.blockY
                        val z = location.blockZ
                        val updateBlocks = arrayOf<Block>(
                            world.getBlockAt(x, y + 1, z), world.getBlockAt(x, y - 1, z),
                            world.getBlockAt(x + 1, y, z), world.getBlockAt(x - 1, y, z),
                            world.getBlockAt(x, y, z - 1), world.getBlockAt(x, y, z + 1)
                        )
                        updateBlocks.forEach {
                            if (it.type == Material.STONE) {
                                val spawn2 = spawnOreAt(it.location.blockY)
                                if (spawn2 != Material.STONE) {
                                    it.type = spawn2
                                }
                            }
                        }
                    }
                }
            } else {
                event.block.world.dropItemNaturally(
                    event.block.location, ItemStack(
                        when (event.block.type) {
                            Material.IRON_ORE -> Material.IRON_INGOT
                            Material.COAL_ORE -> Material.COAL
                            Material.GOLD_ORE -> Material.GOLD_INGOT
                            Material.DIAMOND_ORE -> Material.DIAMOND
                            else -> return
                        }
                    )
                )
            }
        }
    }

    private val pvpRuler = object : GameRuler() {
        override fun getAllowBlockBreaking(): Boolean = true
        override fun getAllowBlockPlacing(): Boolean = true

        override fun getAllowPVP(): Boolean = true

        override fun onEnable() {
            players.forEach {
                it.player.showTitle(
                    title(
                        "PVP时间到！".toWarnMessage(),
                        "您有五分钟时间".toTipMessage(),
                        titleDurationShort
                    )
                )
            }
            (1..2).forEach { t ->
                for (i in 0..1) {
                    for (j in -15..15) {
                        for (y in 256 downTo 4) {
                            if (t == 1) {
                                val b1 = world.getBlockAt(i - 1, y, j).type
                                val b2 = world.getBlockAt(i + 1, y, j).type
                                if ((b1 == Material.BEDROCK || b1 == Material.AIR) && (b2 == Material.BEDROCK || b2 == Material.AIR)) {
                                    world.getBlockAt(i, y, j).type = Material.AIR
                                }
                            } else if (t == 2) {
                                val b1 = world.getBlockAt(j, y, i - 1).type
                                val b2 = world.getBlockAt(j, y, i + 1).type
                                if ((b1 == Material.BEDROCK || b1 == Material.AIR) && (b2 == Material.BEDROCK || b2 == Material.AIR)) {
                                    world.getBlockAt(j, y, i).type = Material.AIR
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun onTimeChanged(i: Long, limit: Long) {
            players.forEach {
                val scoreboard = getScoreboard(it.player, "区块战墙", teamScores!!, i, limit, "PVP")
                it.player.scoreboard = scoreboard
            }
        }

        override fun getTimeLimit(): Long = 5 * 60 * 1000L

        override fun getWorld(): World = world

        override fun getPlayerLocation(info: Gaming.PlayerGamingInfo): Location = info.player.location

        override fun getGameMode(): GameMode = GameMode.SURVIVAL

        private fun detectTeams() {
            val livingTeam = livingTeams
            when (livingTeam.size) {
                1 -> {
                    winner = livingTeam.first()
                    endPeriod(gameID, "只有一个队伍存活")
                }

                0 -> {
                    winner = Team.NONE
                    endPeriod(gameID, "所有队伍被消灭")
                }
            }
        }

        @EventHandler
        fun onPlayerDeath(event: PlayerDeathEvent) {
            if (!validatePlayer(event.entity.player))
                event.keepInventory = true
        }

        @EventHandler
        fun onPlayerRespawn(event: PlayerRespawnEvent) {
            if (!validatePlayer(event.player))
                return
            event.player.showTitle(
                title(
                    "你输了".toErrorMessage(),
                    "请等待其它玩家完成PVP".toTipMessage(),
                    titleDurationMedium
                )
            )
            (players.findInfoByPlayer(event.player) ?: return).gameOver = true
            event.player.gameMode = GameMode.SPECTATOR
            event.respawnLocation = Location(world, 0.toDouble(), 129.toDouble(), 0.toDouble())

            detectTeams()
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitGameEvent) {
            if (!validatePlayer(event.player))
                return
            detectTeams()
        }
    }

    private val resultRuler = object : ResultRuler() {
        override val winner: Team
            get() = this@CW.winner
        override val plugin: JavaPlugin
            get() = this@CW.plugin
        override val isGameStarted: Boolean
            get() = this@CW.isGameStarted

        override fun onEnable() {
            for (x in -16..15) {
                for (z in -16..15) {
                    world.getBlockAt(x, 128, z).type = Material.BARRIER
                }
            }

            super.onEnable()
        }

        override fun getWorld(): World = world
        override fun getPlayerLocation(info: Gaming.PlayerGamingInfo): Location =
            Location(world, 0.toDouble(), 129.toDouble(), 0.toDouble())
    }

    override fun getUnitWinningPrise(): Int = 100
    override fun getWinners(): Array<Player> = resultRuler.winners.toTypedArray()
}