package com.zhufu.opencraft

import com.zhufu.opencraft.util.TextUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

object NPCExtend {
    fun doNPCText(text: String,player: Player){
        //joinGame/TMS
        val separate = text.split('/')
        if (separate.isEmpty())
            throw IllegalArgumentException("Args must not be empty!")
        when(separate.first()){
            "joinGame" -> {
                if (separate.size < 2)
                    throw IllegalArgumentException("There must be at least two args for Command JoinGame!")
                val game = separate[1]
                GameManager.joinPlayerCorrectly(player,game)
            }
            "say" -> {
                if (separate.size < 2)
                    throw IllegalArgumentException("There must be at least two args for Command Say!")
                val content = ArrayList<String>()
                for (i in 1 until separate.size)
                    content.add(TextUtil.tip("NPC -> 你: ") + separate[i])
                player.sendMessage(*content.toTypedArray())
            }
            "teleport" -> {
                if (separate.size < 2)
                    throw IllegalArgumentException("There must be at least two args for Command Teleport!")
                val world = Bukkit.getWorld(separate[1]) ?: throw NullPointerException("World ${separate[1]} not found!")
                if (separate.size == 5) {
                    val x = separate[2].toDouble()
                    val y = separate[3].toDouble()
                    val z = separate[4].toDouble()
                    val dest = Location(world, x, y, z)
                    val event = com.zhufu.opencraft.events.PlayerTeleportedEvent(player, player.location, dest)
                    Bukkit.getPluginManager().callEvent(event)
                    if (!event.isCancelled)
                        player.teleport(dest)
                }
                else{
                    val event = com.zhufu.opencraft.events.PlayerTeleportedEvent(player, player.location, world.spawnLocation)
                    Bukkit.getPluginManager().callEvent(event)
                    if (!event.isCancelled)
                        player.teleport(world.spawnLocation)
                }
            }

        }
    }
}