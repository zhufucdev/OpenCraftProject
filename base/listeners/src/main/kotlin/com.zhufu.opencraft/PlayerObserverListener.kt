package com.zhufu.opencraft

import com.zhufu.opencraft.events.PlayerDeobserveEvent
import com.zhufu.opencraft.events.PlayerObserveEvent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timer

object PlayerObserverListener : Listener {
    private lateinit var mPlugin: Plugin
    fun init(plugin: Plugin){
        mPlugin = plugin
    }
    class ObservablePlayer(val player: Player,val observingPlayers: ArrayList<Player> = ArrayList()){
        var lockTask: Timer = timer( name = "observerLockTask",period = 200L) {
            observingPlayers.forEach {
                Bukkit.getScheduler().runTask(mPlugin) { _ ->
                    if (PlayerManager.findInfoByPlayer(it)?.status == Info.GameStatus.Observing){
                        if (it.gameMode != GameMode.SPECTATOR)
                            it.gameMode = GameMode.SPECTATOR
                        it.spectatorTarget = player
                        it.teleport(player)
                    }
                }
            }
        }
    }
    private val mList = ArrayList<ObservablePlayer>()

    @EventHandler
    fun playerObserver(event: PlayerObserveEvent){
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (info == null){
            event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
            return
        }
        if (info.status == Info.GameStatus.Observing){
            event.player.error("您已经在观战")
            return
        }
        if (info.status == Info.GameStatus.MiniGaming){
            event.player.error("您不能在游戏中观战")
            return
        }
        info.inventory.create(DualInventory.RESET).load(inventoryOnly = true)
        event.player.sendTitle(TextUtil.getColoredText("推荐使用第三人称进行观战", TextUtil.TextColor.AQUA,true,false)
                , TextUtil.getColoredText("同时，若需要退出，使用${TextUtil.tip(mPlugin.server.getPluginCommand("user deobserve")!!.usage)}", TextUtil.TextColor.GOLD,false,false),7,50,7)
        event.onObserver.sendMessage(TextUtil.info("${event.player.name}正在对您进行观战"))

        info.status = Info.GameStatus.Observing

        event.player.gameMode = GameMode.SPECTATOR
        event.player.spectatorTarget = event.onObserver
        event.player.teleport(event.onObserver)

        val t = mList.firstOrNull { it.player == event.player }
        if (t != null){
            if(!t.observingPlayers.contains(event.player))
                t.observingPlayers.add(event.player)
        }
        else{
            mList.add(ObservablePlayer(event.onObserver, arrayListOf(event.player)))
        }
    }

    @EventHandler
    fun playerDeobserve(event: PlayerDeobserveEvent){
        var remove = -1
        mList.forEachIndexed { index, it ->
            it.observingPlayers.remove(event.player)
            if (it.observingPlayers.isEmpty())
                remove = index
        }
        if (remove != -1)
            mList.removeAt(remove)
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (info == null){
            event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
            return
        }
        val lastInventory = info.inventory.last
        info.status = if (lastInventory.name == "survivor") Info.GameStatus.Surviving else Info.GameStatus.InLobby
        lastInventory.load()
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent){
        val p = mList.indexOfFirst { it.player == event.player }
        if (p != -1){
            mList[p].observingPlayers.forEach { playerDeobserve(com.zhufu.opencraft.events.PlayerDeobserveEvent(it)) }
            mList.removeAt(p)
        }
        else{
            mList.firstOrNull { it.observingPlayers.contains(event.player) }?.observingPlayers?.remove(event.player)
        }
    }

    fun onServerStop(){
        mList.forEach {
            it.lockTask.cancel()
        }
    }
}