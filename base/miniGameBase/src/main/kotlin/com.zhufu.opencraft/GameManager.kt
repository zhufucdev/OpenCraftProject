package com.zhufu.opencraft

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

object GameManager {
    private val mList = ArrayList<GameBase>()
    private var plugin: JavaPlugin? = null
    val isInit: Boolean
        get() = plugin != null

    fun init(plugin: JavaPlugin) {
        GameManager.plugin = plugin
    }

    private fun getNewID(): Int {
        var new = 0
        var max = maxID()
        if (max == -1)
            max = 0
        for (i in 0..max + 1) {
            if (mList.any { it.gameID != i }) {
                new = i
                break
            }
        }
        return new
    }

    fun addNew(name: String) {
        mList.add(
            (Class.forName("com.zhufu.opencraft.games.$name").newInstance() as GameBase)
                .also { it.initGame(getNewID(), null, plugin!!) }
        )
    }

    fun joinPlayerCorrectly(player: Player, name: String): GameBase.JoinGameResult {
        var result: GameBase.JoinGameResult = GameBase.JoinGameResult.ROOM_FULL
        mList.forEach {
            if (it.getGameName() == name) {
                result = it.joinPlayer(player)
                if (result == GameBase.JoinGameResult.SUCCESSFUL || result == GameBase.JoinGameResult.ALREADY_IN)
                    return result
            }
        }
        if (result == GameBase.JoinGameResult.ROOM_FULL) {
            val id = getNewID()
            mList.add(
                (Class.forName("com.zhufu.opencraft.games.$name").newInstance() as GameBase)
                    .apply { initGame(id, player, plugin) }
            )
            return joinPlayerTo(id, player)
        }
        return result
    }

    fun joinPlayerTo(id: Int, player: Player): GameBase.JoinGameResult =
        mList.firstOrNull { it.gameID == id }?.joinPlayer(player) ?: GameBase.JoinGameResult.CANCELLED

    private fun maxID(): Int {
        var max = -1
        mList.forEach {
            if (it.gameID > max) {
                max = it.gameID
            }
        }
        return max
    }

    fun forEach(action: (GameBase) -> Unit) = mList.forEach { action(it) }

    fun removeGame(id: Int) {
        println("[GameManager] Removing game IDed $id from list.")
        val index = mList.indexOfFirst { it.gameID == id }
        if (index == -1) {
            println("[GameManager] Game not found.")
            return
        }
        mList.removeAt(index)
    }
}