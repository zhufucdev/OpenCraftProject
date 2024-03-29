package com.zhufu.opencraft

import com.zhufu.opencraft.util.toComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.FireworkMeta
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.time.Duration
import java.util.ArrayList

abstract class ResultRuler : GameRuler() {
    abstract val winner: MiniGame.Team

    override fun getAllowPVP(): Boolean = false

    private var task1: BukkitTask? = null
    private var task2: BukkitTask? = null
    abstract val plugin: JavaPlugin
    abstract val isGameStarted: Boolean
    val winners = ArrayList<Player>()
    override fun onEnable() {
        val titleTimes = Title.Times.times(
            Duration.ofMillis(300),
            Duration.ofSeconds(4),
            Duration.ofMillis(150)
        )
        if (winner != MiniGame.Team.NONE){
            players!!.forEach {
                it.player.scoreboard = Bukkit.getScoreboardManager().newScoreboard
                it.player.showTitle(
                    Title.title(
                        "${winner.name}获得了胜利".toComponent().color(winner.namedTextColor),
                        Component.empty(),
                        titleTimes
                    )
                )
                if (it.team == winner){
                    winners.add(it.player)
                }
            }
            fun setFirework(location: Location){
                val effect = FireworkEffect.builder()
                        .withColor(winner.color)
                        .withTrail()
                        .withFade(Color.YELLOW)
                        .with(FireworkEffect.Type.BALL)
                        .build()
                Bukkit.getScheduler().runTask(plugin) { _ ->
                    val firework = getWorld().spawnEntity(location, EntityType.FIREWORK) as Firework

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
                it.player.showTitle(
                    Title.title(
                        "平局！".toComponent().color(NamedTextColor.GOLD),
                        Component.empty(),
                        titleTimes
                    )
                )
            }
        }
    }

    override fun onDisable() {
        players!!.forEach {
            it.player.scoreboard = Bukkit.getScoreboardManager().newScoreboard
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