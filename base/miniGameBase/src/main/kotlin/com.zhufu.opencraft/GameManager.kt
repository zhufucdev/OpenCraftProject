package com.zhufu.opencraft

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object GameManager {
    private val mList = arrayListOf<MiniGame>()
    private val registration = mutableSetOf<KClass<*>>()
    private lateinit var plugin: JavaPlugin
    val isInit: Boolean
        get() = ::plugin.isInitialized

    fun init(plugin: JavaPlugin) {
        GameManager.plugin = plugin
    }

    fun registerGameType(clazz: KClass<*>) {
        registration.add(clazz)
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

    private fun constructGame(name: String): MiniGame {
        return (registration.firstOrNull { it.simpleName == name }?.primaryConstructor?.call() as MiniGame?)
            ?.also { it.initGame(getNewID(), null, plugin) }
            ?: throw NullPointerException("$name isn't in registration.")
    }

    fun addNew(name: String) {
        mList.add(constructGame(name))
    }

    fun joinPlayerCorrectly(player: Player, name: String): MiniGame.JoinGameResult {
        var result: MiniGame.JoinGameResult = MiniGame.JoinGameResult.ROOM_FULL
        mList.forEach {
            if (it.getGameName() == name) {
                result = it.joinPlayer(player)
                if (result == MiniGame.JoinGameResult.SUCCESSFUL || result == MiniGame.JoinGameResult.ALREADY_IN)
                    return result
            }
        }
        if (result == MiniGame.JoinGameResult.ROOM_FULL) {
            val id = getNewID()
            mList.add(
                constructGame(name)
                    .apply { initGame(id, player, plugin) }
            )
            return joinPlayerTo(id, player)
        }
        return result
    }

    fun joinPlayerTo(id: Int, player: Player): MiniGame.JoinGameResult =
        mList.firstOrNull { it.gameID == id }?.joinPlayer(player) ?: MiniGame.JoinGameResult.CANCELLED

    private fun maxID(): Int {
        var max = -1
        mList.forEach {
            if (it.gameID > max) {
                max = it.gameID
            }
        }
        return max
    }

    fun forEach(action: (MiniGame) -> Unit) = mList.forEach { action(it) }

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