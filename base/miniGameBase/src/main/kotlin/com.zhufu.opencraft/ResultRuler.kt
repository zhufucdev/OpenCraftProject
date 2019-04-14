package com.zhufu.opencraft

import org.bukkit.*
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftFirework
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.ArrayList

abstract class ResultRuler() : GameRuler() {
    abstract val winner: GameBase.Team

    override fun getAllowPVP(): Boolean = false

    var task1: BukkitTask? = null
    var task2: BukkitTask? = null
    abstract val plugin: JavaPlugin
    abstract val isGameStarted: Boolean
    val winners = ArrayList<Player>()
    override fun onEnable() {
        if (winner != GameBase.Team.NONE){
            players!!.forEach {
                it.player.scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
                it.player.sendTitle(TextUtil.getColoredText("${winner.name}获得了胜利",winner.getTextColor(),true,false),"",7,80,7)
                if (it.team == winner){
                    winners.add(it.player)
                }
            }
            fun setFirework(location: Location){
                val effect = FireworkEffect.builder()
                        .withColor(winner.getColor())
                        .withTrail()
                        .withFade(Color.YELLOW)
                        .with(FireworkEffect.Type.BALL)
                        .build()
                Bukkit.getScheduler().runTask(plugin) { _ ->
                    val firework = getWorld().spawnEntity(
                            location,
                            EntityType.FIREWORK
                    ) as CraftFirework

                    val data = firework.fireworkMeta
                    data.power = 2
                    data.addEffect(effect)
                    firework.fireworkMeta = data
                }
            }
            task1 = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                while (isGameStarted) {
                    setFirework(Base.getRandomLocation(location = getWorld().spawnLocation, y = 0, bound = 10))
                    Thread.sleep(700)
                }
            })
            task2 = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable{
                while (isGameStarted){
                    winners.forEach {
                        setFirework(Base.getRandomLocation(location = it.location, y = 0, bound = 10))
                    }
                    Thread.sleep(700)
                }
            })
        }
        else{
            players!!.forEach {
                it.player.sendTitle(TextUtil.getColoredText("平局！", TextUtil.TextColor.GOLD,true,false),"",7,80,7)
            }
        }
    }

    override fun onDisable() {
        players!!.forEach {
            it.player.scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
        }

        if (task1 != null)
            task1!!.cancel()
        if (task2 != null)
            task2!!.cancel()
    }

    override fun onTimeChanged(i: Long, limit: Long) {}

    override fun getTimeLimit(): Long = 10*1000L

    override fun getGameMode(): GameMode = GameMode.ADVENTURE
}