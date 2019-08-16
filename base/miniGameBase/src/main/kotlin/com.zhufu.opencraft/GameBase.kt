package com.zhufu.opencraft

import com.zhufu.opencraft.DualInventory.Companion.NOTHING
import com.zhufu.opencraft.DualInventory.Companion.RESET
import com.zhufu.opencraft.events.PeriodEndEvent
import com.zhufu.opencraft.events.PlayerJoinGameEvent
import com.zhufu.opencraft.events.PlayerQuitGameEvent
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import org.bukkit.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToInt

abstract class GameBase : Listener {
    val gaming = Gaming()
    var teamScores: TeamScores? = null
    var gameID = -1

    val realTeams: Array<Team>
        get() {
            val r = ArrayList<Team>()
            for (team in getTeams()) {
                var has = false
                for (player in gaming) {
                    if (player.team == team) {
                        has = true
                        break
                    }
                }
                if (has)
                    r.add(team)
            }
            return r.toTypedArray()
        }
    val livingTeamCount: Int
        get() {
            var r = 0
            realTeams.forEach { if (!gaming.isTeamDead(it)) r++ }
            return r
        }
    val livingTeams: List<Team>
        get() {
            val r = ArrayList<Team>()
            realTeams.forEach { if (!gaming.isTeamDead(it)) r.add(it) }
            return r.toList()
        }
    val isRoomFull: Boolean
        get() = gaming.size >= getMaxPlayerCount() || isGameStarted

    private var waitingTimer: Timer? = null
    private var gamingTimer: Timer? = null
    private var periodIndex: Int = 0
    var isGameStarted = false

    private var worlds = ArrayList<World>()
    lateinit var plugin: JavaPlugin

    /* Game Methods */
    open fun initGame(id: Int, player: Player?, plugin: JavaPlugin) {
        this.plugin = plugin
        plugin.server.pluginManager.registerEvents(this, plugin)
        if (id < 0) {
            throw IllegalAccessException("Illegal ID (ID < 0)!")
        } else {
            this.gameID = id
        }
    }

    private fun nextPeriod() {
        if (gamingTimer != null) {
            gamingTimer!!.cancel()
            gamingTimer = null
        }
        if (periodIndex + 1 > getGameRulers().size) {
            Bukkit.getScheduler().runTask(plugin) { _ ->
                initGameEnder()
            }
            return
        }

        val currentRuler = getGameRulers()[periodIndex]
        isGameStarted = true

        val scheduler = Bukkit.getScheduler()
        plugin.server.pluginManager.registerEvents(currentRuler, plugin)

        currentRuler.getWorld()
            .also { if (!worlds.contains(it)) worlds.add(it) }
            .pvp = currentRuler.getAllowPVP()
        currentRuler.players = gaming

        currentRuler.onEnable()

        gaming.forEach {
            it.player.gameMode = currentRuler.getGameMode()
            it.player.teleport(currentRuler.getPlayerLocation(it))
        }

        var i = 0L
        gamingTimer = fixedRateTimer(name = "periodTimer", period = 100L) {
            /**
             * Call [onTimeChanged]
             */
            if (i * 100L >= currentRuler.getTimeLimit()) {
                //When reaching TimeLimit, call onDisabled()
                scheduler.runTask(plugin) { _ ->
                    //Disable last ruler.
                    currentRuler.onDisable()
                    HandlerList.unregisterAll(currentRuler)

                    scheduler.runTask(plugin) { _ ->
                        nextPeriod()
                    }
                }
            }
            scheduler.runTask(plugin) { _ ->
                currentRuler.onTimeChanged(i, currentRuler.getTimeLimit() / 100L)
            }
            i++
        }

        periodIndex++
    }

    private fun initWaitTimer() {
        fun start() {
            teamScores = TeamScores(realTeams)
            nextPeriod()
        }
        if (gaming.size < getMaxPlayerCount()) {
            if (waitingTimer != null) {
                return
            }

            var i = 0
            waitingTimer = fixedRateTimer(name = "GameStartingTimer", period = 10 * 1000L) {
                if (i >= 3 && gaming.size >= getPlayerCount()) {
                    this.cancel()
                    Bukkit.getScheduler().runTask(plugin) { _ ->
                        this.cancel()
                        start()
                    }
                }
                broadcast("game.waiting", 30 - 10 * i)
                i++
            }
        } else {
            broadcast("game.upcoming")
            Bukkit.getScheduler().runTaskLater(plugin, { _ ->
                start()
            }, 20L)
        }
    }

    private fun onWinnerResultShow() {
        val winners = getWinners()
        if (winners.isNotEmpty()) {
            val prise = ((gaming.size - winners.size) * getUnitWinningPrise() / winners.size.toDouble()).roundToInt()
            winners.forEach {
                val info = PlayerManager.findInfoByPlayer(it)
                if (info == null) {
                    it.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
                } else {
                    it.sendMessage(TextUtil.success(Language[info, "game.win"]))
                    it.sendMessage(TextUtil.info(Language[info, "game.win.result", winners.size, gaming.size - winners.size, getUnitWinningPrise(), prise]))

                    info.currency += prise
                }
            }
        }
    }

    fun initGameEnder() {
        onWinnerResultShow()

        isGameStarted = false
        plugin.logger.info("Ending game IDed $gameID")

        if (gamingTimer != null) {
            gamingTimer!!.cancel()
            gamingTimer = null
        }
        if (waitingTimer != null) {
            waitingTimer!!.cancel()
            waitingTimer = null
        }

        getGameRulers().forEach {
            HandlerList.unregisterAll(it)
        }
        HandlerList.unregisterAll(this)

        gaming.forEach { info ->
            println("[${getGameName()}] Unloading player ${info.player.displayName}")
            info.player.isInvulnerable = false
            info.player.scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
            PlayerManager.findInfoByPlayer(info.player)
                ?.also {
                    it.player.sendMessage(TextUtil.info(Language[it, "game.reset"]))
                    it.status = Info.GameStatus.InLobby
                }
                ?.inventory?.create(RESET)?.load()
        }
        gaming.clear()

        //Destroy the save
        worlds.forEach { world ->
            Bukkit.unloadWorld(world, false)
        }
        worlds.clear()

        GameManager.removeGame(gameID)
        gameID = -1
    }

    /* Player Join & Quit methods */
    enum class JoinGameResult {
        SUCCESSFUL, ALREADY_IN, ROOM_FULL, FAILED, CANCELLED
    }

    fun joinPlayer(player: Player): JoinGameResult {
        val info = player.info()
        val getter = info.getter()
        if (info == null) {
            player.error(getter["player.error.unknown"])
            return JoinGameResult.FAILED
        }
        if (gaming.contains(player)) {
            player.error(getter["game.error.alreadyIn"])
            return JoinGameResult.ALREADY_IN
        } else if (isRoomFull) {
            info.status = Info.GameStatus.InLobby
            player.error(getter["game.error.roomFull"])
            return JoinGameResult.ROOM_FULL
        }
        val event = PlayerJoinGameEvent(player, gameID)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled)
            return JoinGameResult.CANCELLED

        gaming.add(player, team = getSuitablePlayerTeam())
        broadcast("player.joinGame", "(${gaming.size}/${getMaxPlayerCount()}) ${player.name}", getGameName())
        //Load player profile
        info.inventory.create(NOTHING).load()
        DualInventory.resetPlayer(player)
        info.status = Info.GameStatus.MiniGaming

        if (gaming.size >= getPlayerCount()) {
            initWaitTimer()
        }
        player.teleport(
            getWaitingRoom(player)
                .also {
                    //Disable PVP
                    it.world!!.pvp = false
                }
        )
        return JoinGameResult.SUCCESSFUL
    }

    fun kickPlayer(player: Player) {
        gaming.remove(player)
        broadcast("player.joinGame", "(${gaming.size}/${getMaxPlayerCount()}) ${player.displayName}")

        if (gaming.size < getPlayerCount() && waitingTimer != null) {
            waitingTimer!!.cancel()
            waitingTimer = null

            broadcast("game.cancelTimer")
        }

        if (player.isInvulnerable) {
            player.isInvulnerable = false
        }
        player.setGameName(Team.NONE)
        PlayerManager.findInfoByPlayer(player)
            ?.also { it.status = Info.GameStatus.InLobby }
            ?.inventory?.create(RESET)?.load()
    }

    private fun getSuitablePlayerTeam(): Team {
        val teamCount = Array(Team.values().size) { 0 }
        val teams = getTeams()

        teams.forEach { team ->
            gaming.forEach {
                if (it.team == team) {
                    teamCount[team.ordinal]++
                }
            }
        }
        /* Find the team that has the fewest players */
        var minIndex = teams.first().ordinal
        var min = teamCount[minIndex]
        for (i in 1 until teamCount.size) {
            if (teamCount[i] < min) {
                min = teamCount[i]
                minIndex = i
            }
        }
        return Team.getValueByOrdinal(minIndex)
    }

    /**
     * Call when a player joins.
     * @return The location where the player should be waiting at.
     */
    abstract fun getWaitingRoom(player: Player): Location

    /**
     * @return How many and what kinds of teams should the game have.
     */
    abstract fun getTeams(): Array<Team>

    /**
     * @return How many players does the game need.
     */
    abstract fun getPlayerCount(): Int

    /**
     * @return How many players should the game have at most.
     */
    abstract fun getMaxPlayerCount(): Int

    /**
     * @return The game mode the player should be in after game starts.
     */
    abstract fun getDefaultGameMode(): GameMode

    /**
     * The GameRuler
     */
    abstract fun getGameRulers(): Array<GameRuler>

    /**
     * The label of this type of game
     */
    abstract fun getGameName(): String

    /**
     * Call when the game ends
     */
    abstract fun getWinners(): Array<Player>

    abstract fun getUnitWinningPrise(): Int

    /* Game Event Listener */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitGameEvent) {
        if (event.which != gameID)
            return
        kickPlayer(event.player)

        if (livingTeamCount == 0) {
            initGameEnder()
        }
    }

    @EventHandler
    fun onPlayerQuitServer(event: PlayerQuitEvent) {
        if (validatePlayer(event.player)) {
            Bukkit.getPluginManager().callEvent(PlayerQuitGameEvent(event.player, gameID))
        }
    }

    @EventHandler
    fun onPlayerTeleported(event: PlayerTeleportedEvent) {
        if (!validatePlayer(event.player))
            return
        Bukkit.getPluginManager().callEvent(PlayerQuitGameEvent(event.player, gameID))
    }

    @EventHandler
    fun onEndPeriod(event: PeriodEndEvent) {
        if (event.which != gameID)
            return
        Bukkit.getScheduler().runTask(plugin) { _ ->
            broadcast(TextUtil.info("进行下一阶段，因为${event.cause}"))
            nextPeriod()
        }
    }

    @EventHandler
    fun onPlayerBreakBlock(event: BlockBreakEvent) {
        if (!validatePlayer(event.player))
            return
        if (!getGameRulers()[periodIndex].getAllowBlockBreaking()) {
            event.isCancelled = true
            event.player.sendMessage(TextUtil.info("此阶段不允许方块破坏"))
        }
    }

    @EventHandler
    fun onPlayerPlaceBlock(event: BlockPlaceEvent) {
        if (!validatePlayer(event.player))
            return
        if (!getGameRulers()[periodIndex].getAllowBlockPlacing()) {
            event.isCancelled = true
            event.player.sendMessage(TextUtil.info("此阶段不允许方块放置"))
        }
    }

    /* Extended Functions */
    fun broadcast(msg: String, vararg replace: Any?) {
        gaming.forEach {
            val info = PlayerManager.findInfoByPlayer(it.player)
            if (info != null)
                it.player.sendMessage(TextUtil.info(Language.got(info.targetLang, msg, replace)))
            else
                it.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
        }
    }

    fun resetPlayer(player: Player) {
        player.health = 20.toDouble()
        player.foodLevel = 25
        player.exp = 0f
        PlayerManager.findInfoByPlayer(player)?.inventory?.create(RESET)?.load()
    }

    /**
     * Call this before EventHandler
     */
    fun validatePlayer(player: Player) = gaming.contains(player)

    enum class Team(i: Int) {
        RED(0), BLUE(1), GREEN(2), YELLOW(3), PINK(4), PURPLE(5), NONE(-1);

        fun getTextColor() = getTextColorByObject(this)
        fun getColor() = getColorByObject(this)

        companion object {
            fun getValueByOrdinal(i: Int): Team = values().first { it.ordinal == i }
            fun getTextColorByObject(i: Team): TextUtil.TextColor = when (i) {
                RED -> TextUtil.TextColor.RED
                BLUE -> TextUtil.TextColor.BLUE
                GREEN -> TextUtil.TextColor.GREEN
                YELLOW -> TextUtil.TextColor.YELLOW
                PINK -> TextUtil.TextColor.LIGHT_PURPLE
                PURPLE -> TextUtil.TextColor.DARK_PURPLE
                else -> TextUtil.TextColor.WHITE
            }

            fun getColorByObject(i: Team): Color = when (i) {
                RED -> Color.RED
                BLUE -> Color.BLUE
                GREEN -> Color.GREEN
                YELLOW -> Color.YELLOW
                PINK -> Color.PURPLE
                PURPLE -> Color.PURPLE
                else -> Color.WHITE
            }
        }
    }

    class Gaming : ArrayList<Gaming.PlayerGamingInfo>() {
        fun add(element: Player, team: Team): Boolean {
            return super.add(PlayerGamingInfo(element, team))
        }

        fun findInfoByPlayer(player: Player) = this.firstOrNull { it.player == player }
        fun remove(player: Player) = this.removeAll { it.player == player }
        fun contains(player: Player) = findInfoByPlayer(player) != null
        override fun clear() {
            forEach {
                it.player.setGameName(Team.NONE)
                super.remove(it)
            }
        }

        fun isTeamDead(which: Team): Boolean {
            var r = true
            for (info in this) {
                if ((info.team == which && !info.gameOver)) {
                    r = false
                    break
                }
            }
            return r
        }

        class PlayerGamingInfo(val player: Player, val team: Team) {
            var tag = YamlConfiguration()
            var gameOver = false

            init {
                player.setGameName(team)
            }

            override fun equals(other: Any?): Boolean {
                return (other is PlayerGamingInfo)
                        && (other.player == this.player && other.team == this.team)
            }

            override fun hashCode(): Int {
                var result = player.hashCode()
                result = 31 * result + team.hashCode()
                result = 31 * result + tag.hashCode()
                return result
            }
        }
    }

    class TeamScores(teams: Array<Team>) : ArrayList<TeamScores.TeamScore>() {
        init {
            teams.forEach { super.add(TeamScore(it, 0)) }
        }

        fun set(team: Team, s: ((score: Int) -> Int)): Boolean {
            return try {
                this.first { it.team == team }.score = s.invoke(this.first { it.team == team }.score)
                true
            } catch (e: NoSuchElementException) {
                false
            }
        }

        fun get(element: Team): TeamScore? = try {
            this.first { it.team == element }
        } catch (e: NoSuchElementException) {
            null
        }

        fun max(): Team {
            var max = this.first().score
            var index = 0
            for (i in 1 until this.size) {
                if (super.get(i).score > max) {
                    max = super.get(i).score
                    index = i
                }
            }
            return super.get(index).team
        }

        class TeamScore(val team: Team, var score: Int) {
            override fun equals(other: Any?): Boolean {
                return (other is TeamScore)
                        && (other.team == this.team && other.score == this.score)
            }

            override fun hashCode(): Int {
                var result = team.hashCode()
                result = 31 * result + score.hashCode()
                return result
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other is GameBase)
                && (other.gameID == this.gameID)
    }

    override fun hashCode(): Int {
        var result = gaming.hashCode()
        result = 31 * result + (teamScores?.hashCode() ?: 0)
        result = 31 * result + gameID
        result = 31 * result + periodIndex
        result = 31 * result + isGameStarted.hashCode()
        result = 31 * result + worlds.hashCode()
        return result
    }

    companion object {
        fun getScoreboard(
            player: Player,
            title: String,
            teamScores: TeamScores,
            time: Long,
            limit: Long,
            what: String
        ): Scoreboard {
            val scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
            val scoreboardObjective = scoreboard.registerNewObjective(title, "dummy", title)
            scoreboardObjective.displaySlot = DisplaySlot.SIDEBAR

            scoreboardObjective.getScore(TextUtil.info(Language[player, "game.teamScore"])).score = 10000
            for (team in teamScores) {
                scoreboardObjective.getScore(
                    "  ${TextUtil.getColoredText(
                        team.team.name,
                        team.team.getTextColor(),
                        true,
                        false
                    )}"
                )
                    .score = team.score
            }
            scoreboardObjective.getScore(
                TextUtil.info(
                    Language[player, "game.timeRemainingInSec", what, (limit - time).div(
                        10
                    )]
                )
            ).score = -10000

            return scoreboard
        }

        fun Player.setGameName(team: Team) {
            customName = team.getTextColor().code + player!!.name + TextUtil.END
            setPlayerListName(player!!.customName)
            setDisplayName(player!!.customName)
        }
    }
}