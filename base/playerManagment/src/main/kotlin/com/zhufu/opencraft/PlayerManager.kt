package com.zhufu.opencraft

import com.zhufu.opencraft.Info.Companion.cache
import com.zhufu.opencraft.Info.Companion.plugin
import com.zhufu.opencraft.OfflineInfo.Companion.cacheList
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.collections.ArrayList

object PlayerManager : Listener {
    private val chatters = ArrayList<ChatInfo>()
    private lateinit var plugin: JavaPlugin
    val isInit: Boolean
        get() = ::plugin.isInitialized

    fun init(plugin: JavaPlugin) {
        PlayerManager.plugin = plugin
        Info.plugin = plugin
    }

    fun add(chatInfo: ChatInfo) {
        if (!chatters.contains(chatInfo))
            chatters.add(chatInfo)
    }

    fun remove(chatInfo: ChatInfo) = chatters.remove(chatInfo)
    fun findChatter(name: String) = chatters.firstOrNull { it.id == name } ?: cache.firstOrNull { it.name == name }
    fun removeFirstChatter(l: (ChatInfo) -> Boolean): Boolean {
        val index = chatters.indexOfFirst(l)
        if (index == -1)
            return false
        chatters.removeAt(index)
        return true
    }

    fun findInfoByPlayer(player: Player) = Info.findByPlayer(player)
    fun findInfoByPlayer(uuid: UUID) = Info.findByPlayer(uuid)
    fun findOfflinePlayer(uuid: UUID): OfflineInfo? = cache.firstOrNull { it.uuid == uuid }
        ?: cacheList.firstOrNull { it.uuid == uuid }
        ?: try {
            OfflineInfo(uuid).also { cacheList.add(it) }
        } catch (e: Exception) {
            null
        }

    fun createOfflinePlayer(uuid: UUID) = findOfflinePlayer(uuid)
        ?: OfflineInfo(uuid, true).also { cacheList.add(it) }

    fun forEachPlayer(l: (Info) -> Unit) = cache.forEach(l)
    fun forEachChatter(l: (ChatInfo) -> Unit) {
        cache.forEach(l)
        chatters.forEach(l)
    }

    fun forEachOffline(l: (OfflineInfo) -> Unit) = cacheList.forEach(l)

    fun addOffline(info: OfflineInfo) = cacheList.add(info)
    fun add(info: Info) = cache.add(info)
    fun remove(p: Player) = forEachPlayer {
        if (it.uuid == p.uniqueId) {
            it.destroy()
        }
    }

    fun onPlayerOutOfDemo(info: Info) {
        val event = PlayerTeleportedEvent(info.player, info.player.location, Base.lobby.spawnLocation)
        Bukkit.getPluginManager().callEvent(event)
        if (!event.isCancelled) {
            info.player.teleport(Base.lobby.spawnLocation)
            info.status = Info.GameStatus.InLobby
        }
        showPlayerOutOfDemoTitle(info.player)
    }

    fun showPlayerOutOfDemoTitle(player: Player) {
        val getter = player.getter()
        player.sendTitle(TextUtil.info(getter["survey.title"]), TextUtil.tip(getter["survey.toBeMember"]), 7, 80, 7)
    }
}

fun broadcast(value: String, color: TextUtil.TextColor, vararg replaceWith: String?) {
    val langMap = HashMap<String, String>()
    PlayerManager.forEachChatter {
        if (it is Info && !it.isLogin) return@forEachChatter
        val lang = it.targetLang
        if (!langMap.containsKey(lang)) {
            langMap[lang] = TextUtil.getColoredText(
                Language.got(lang, value, replaceWith), color,
                bold = false,
                underlined = false
            )
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
            it.playerOutputStream.send(langMap[lang]!!)
        }
    }
}