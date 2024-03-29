package com.zhufu.opencraft.games

import com.zhufu.opencraft.*
import com.zhufu.opencraft.events.PlayerQuitGameEvent
import com.zhufu.opencraft.util.toErrorMessage
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toTipMessage
import com.zhufu.opencraft.util.toWarnMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.block.Biome
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import java.util.*
import kotlin.concurrent.timer

class TMS : MiniGame() {
    private var winner: Team = Team.NONE
    lateinit var world: World

    override fun initGame(id: Int, player: Player?, plugin: JavaPlugin) {
        super.initGame(id, player, plugin)
        player?.sendMessage("正在创建新的房间，请稍后".toInfoMessage())
        world = Bukkit.createWorld(WorldCreator("game_tms_$id"))!!
    }

    override fun getGameRulers(): Array<GameRuler> = arrayOf(gameRuler, pvpRuler, resultRuler)

    override fun getGameName(): String = "TMS"

    private val gameRuler = object : GameRuler() {
        private lateinit var locations: Array<Location>

        override fun getAllowBlockBreaking(): Boolean = true
        override fun getAllowBlockPlacing(): Boolean = true

        override fun getAllowPVP(): Boolean = true

        override fun getTimeLimit(): Long = 10 * 60 * 1000L

        override fun getWorld(): World = world

        override fun getPlayerLocation(info: Gaming.PlayerGamingInfo): Location = locations[info.team.ordinal]

        override fun getGameMode(): GameMode = GameMode.SURVIVAL

        override fun onEnable() {
            world.time = Random().nextInt(10000).toLong()
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true)

            locations = Array(getTeams().size) {
                var r = Base.getRandomLocation(world, 1000, y = 128)
                var b = world.getBiome(r.blockX, world.getHighestBlockYAt(r), r.blockZ)
                while (b == Biome.DESERT || b == Biome.OCEAN || b == Biome.DEEP_OCEAN || b == Biome.MUSHROOM_FIELDS) {
                    r.chunk.unload(false)
                    r = Base.getRandomLocation(world, 1000, y = 128)
                    b = world.getBiome(r.blockX, world.getHighestBlockYAt(r), r.blockZ)
                }
                r
            }

            players.forEach {
                it.player.showTitle(
                    Title.title(
                        "游戏开始".toWarnMessage(),
                        "您有十分钟的时间生存".toTipMessage(),
                        titleDurationShort
                    )
                )
                it.player.isInvulnerable = true
            }
        }

        @EventHandler
        fun onPlayerMove(event: PlayerMoveEvent) {
            if (!validatePlayer(event.player))
                return
            val tag = players.findInfoByPlayer(event.player)?.tag ?: return
            if (!tag.getBoolean("isUnprotected", false) && event.to.clone().add(
                    Vector(
                        0,
                        -1,
                        0
                    )
                ).block.type != Material.AIR
            ) {
                tag.set("isUnprotected", true)
                if (validatePlayer(event.player)) {
                    timer(name = "unprotectionTimer", period = 10 * 1000L) {
                        plugin.logger.info("${event.player.name} lands.")
                        event.player.showTitle(
                            Title.title(
                                "您已失去出生保护".toInfoMessage(),
                                Component.empty(),
                                titleDurationShort
                            )
                        )
                        event.player.isInvulnerable = false
                        this.cancel()
                    }
                }

            }
        }

        @EventHandler
        fun playerRespawn(event: PlayerRespawnEvent) {
            val player = event.player
            if (!validatePlayer(player))
                return
            player.gameMode = GameMode.SPECTATOR
            player.showTitle(
                Title.title(
                    "您输了".toErrorMessage(),
                    "现在处于旁观者".toInfoMessage(),
                    titleDurationMedium
                )
            )
            players.findInfoByPlayer(event.player)?.gameOver = true
            event.respawnLocation = world.spawnLocation

            //When all the team members are dead
            if (livingTeamCount == 0) {
                endPeriod(gameID, "所有队伍都被消灭")
            }
        }

        @EventHandler
        fun playerDeath(event: PlayerDeathEvent) {
            if (!validatePlayer(event.entity.player))
                return
            teamScores!!.set((players.findInfoByPlayer(event.entity.player!!) ?: return).team)
            { it - 1 }
            event.keepInventory = true
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitGameEvent) {
            if (!validatePlayer(event.player))
                return
            when (livingTeamCount) {
                1 -> {
                    endPeriod(gameID, "只有一个队伍生存")
                }
                0 -> {
                    endPeriod(gameID, "全部队伍被消灭")
                }
            }
        }

        @EventHandler
        fun onPlayerBreakBlock(event: BlockBreakEvent) {
            val player = event.player
            if (!validatePlayer(player))
                return

            val block = event.block
            teamScores!!.set((players.findInfoByPlayer(player) ?: return).team) {
                when (block.type) {
                    Material.IRON_ORE -> it + 4
                    Material.COAL -> it + 1
                    Material.GOLD_ORE -> it + 6
                    Material.DIAMOND_ORE -> it + 12
                    else -> it
                }
            }
        }

        override fun onTimeChanged(i: Long, limit: Long) {
            players.forEach {
                val scoreboard = getScoreboard(it.player, "十分钟生存挑战", teamScores!!, i, limit, "生存")
                it.player.scoreboard = scoreboard
            }
        }
    }

    private val pvpRuler = object : GameRuler() {
        override fun getAllowBlockBreaking(): Boolean = true
        override fun getAllowBlockPlacing(): Boolean = true

        override fun getAllowPVP(): Boolean = true

        override fun onEnable() {
            detectTeams()
            players.forEach {
                val player = it.player
                player.teleport(world.spawnLocation)
                player.showTitle(
                    Title.title(
                        "PVP时间到！".toWarnMessage(),
                        Component.empty(),
                        titleDurationShort
                    )
                )
                if (player.isInvulnerable) {
                    player.isInvulnerable = false
                }
            }
        }

        private fun detectTeams() {
            when (livingTeamCount) {
                1 -> {
                    winner = teamScores!!.max()
                    endPeriod(gameID, "只有一个队伍生存")
                }
                0 -> {
                    winner = Team.NONE
                    endPeriod(gameID, "全部队伍被消灭")
                }
            }
        }

        @EventHandler
        fun onPlayerRespawn(event: PlayerRespawnEvent) {
            val player = event.player
            if (!validatePlayer(player))
                return

            event.respawnLocation = world.spawnLocation
            player.gameMode = GameMode.SPECTATOR
            player.showTitle(
                Title.title(
                    "你输了".toErrorMessage(),
                    "现在处于傍观者".toInfoMessage(),
                    titleDurationLong
                )
            )
            players.findInfoByPlayer(event.player)?.gameOver = true
            Bukkit.getScheduler().runTask(plugin) { _ ->
                player.teleport(world.spawnLocation)
            }
            detectTeams()
        }

        @EventHandler
        fun onPlayerDeath(event: PlayerDeathEvent) {
            if (!validatePlayer(event.entity.player))
                return
            teamScores!!.set((players.findInfoByPlayer(event.entity.player!!) ?: return).team)
            { it - 2 }
            event.keepInventory = true
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitGameEvent) {
            if (!validatePlayer(event.player))
                return
            detectTeams()
        }

        override fun onTimeChanged(i: Long, limit: Long) {
            players.forEach {
                val scoreboard = getScoreboard(it.player, "十分钟生存挑战", teamScores!!, i, limit, "PVP")
                it.player.scoreboard = scoreboard
            }
        }

        override fun getTimeLimit(): Long = 5 * 60 * 1000L

        override fun getWorld(): World = world

        override fun getPlayerLocation(info: Gaming.PlayerGamingInfo): Location = world.spawnLocation

        override fun getGameMode(): GameMode = GameMode.SURVIVAL
    }

    private val resultRuler = object : ResultRuler() {
        override val winner: Team
            get() = this@TMS.winner
        override val plugin: JavaPlugin
            get() = this@TMS.plugin
        override val isGameStarted: Boolean
            get() = this@TMS.isGameStarted

        override fun onEnable() {
            players.forEach {
                it.player.teleport(it.player.location.add(0.toDouble(), 10.toDouble(), 0.toDouble()))
                it.player.isInvulnerable = true
            }
            super.onEnable()
        }

        override fun getPlayerLocation(info: Gaming.PlayerGamingInfo): Location = world.spawnLocation

        override fun getWorld(): World = this@TMS.world
    }

    override fun getWaitingRoom(player: Player): Location = world.spawnLocation

    override fun getTeams(): Array<Team> = arrayOf(Team.BLUE, Team.RED, Team.GREEN, Team.YELLOW)

    override fun getPlayerCount(): Int = 2

    override fun getMaxPlayerCount(): Int = 20

    override fun getDefaultGameMode(): GameMode = GameMode.SURVIVAL

    override fun getWinners(): Array<Player> = resultRuler.winners.toTypedArray()

    override fun getUnitWinningPrise(): Int = 100
}