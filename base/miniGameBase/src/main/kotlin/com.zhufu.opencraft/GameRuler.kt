package com.zhufu.opencraft

import com.zhufu.opencraft.events.PeriodEndEvent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.Listener

abstract class GameRuler : Listener {
    var players: GameBase.Gaming? = null

    open fun getAllowBlockBreaking(): Boolean = false
    open fun getAllowBlockPlacing(): Boolean = false
    abstract fun getAllowPVP(): Boolean

    abstract fun onEnable()

    open fun onDisable(){}

    fun endPeriod(which: Int,cause: String){
        onDisable()
        Bukkit.getPluginManager().callEvent(PeriodEndEvent(which, cause))
    }

    /**
     * Call per 0.1s
     */
    abstract fun onTimeChanged(i: Long,limit: Long)

    /**
     * @return How long is it between the period starts and ends.
     */
    abstract fun getTimeLimit(): Long

    /**
     * Call when ruler is enabled.
     * @return The world for this ruler.
     */
    abstract fun getWorld(): World

    /**
     * @return The location should the players in this period spawn at.
     */
    abstract fun getPlayerLocation(info: GameBase.Gaming.PlayerGamingInfo): Location

    abstract fun getGameMode(): GameMode

    /**
     * Call this before EventHandler
     */
    fun validatePlayer(player: Player?) = player != null && players?.contains(player) ?:false
}