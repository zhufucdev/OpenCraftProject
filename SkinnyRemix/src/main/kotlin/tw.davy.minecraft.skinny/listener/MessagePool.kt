package tw.davy.minecraft.skinny.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

import java.util.ArrayList
import java.util.HashMap
import java.util.UUID

import org.bukkit.Bukkit.getServer

class MessagePool : Listener {

    private val messageMap = HashMap<UUID, ArrayList<String>>()

    init {
        instance = this
    }

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        if (messageMap.containsKey(event.player.uniqueId)) {
            messageMap.getOrDefault(event.player.uniqueId, ArrayList()).forEach { String -> event.player.sendMessage(String) }
            messageMap.remove(event.player.uniqueId)
        }
    }

    internal fun sendMessageToPlayer(uuid: UUID, msg: String) {
        for (player in getServer().onlinePlayers) {
            if (player.uniqueId === uuid) {
                player.sendMessage(msg)
                return
            }
        }
        if (!messageMap.containsKey(uuid)) {
            messageMap[uuid] = ArrayList()
        }
        messageMap[uuid]!!.add(msg)
    }

    companion object {
        lateinit var instance: MessagePool
    }
}
