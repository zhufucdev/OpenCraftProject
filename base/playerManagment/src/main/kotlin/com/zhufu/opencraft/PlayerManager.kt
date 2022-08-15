@file:Suppress("unused")

package com.zhufu.opencraft

import com.zhufu.opencraft.api.ChatInfo
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.data.Info.Companion.plugin
import com.zhufu.opencraft.data.OfflineInfo
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.TextUtil
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
    fun removeFirstChatter(l: (ChatInfo) -> Boolean): Boolean {
        val index = chatters.indexOfFirst(l)
        if (index == -1)
            return false
        chatters.removeAt(index)
        return true
    }

    fun findInfoByPlayer(player: Player) = Info.findByPlayer(player)
    fun findInfoByPlayer(uuid: UUID) = Info.findByPlayer(uuid)
    fun findInfoByName(name: String) = Info.findByName(name)
    fun findOfflineInfoByPlayer(uuid: UUID): OfflineInfo? = Info.cache.firstOrNull { it.uuid == uuid }
        ?: OfflineInfo.cache.firstOrNull { it.uuid == uuid }
        ?: try {
            OfflineInfo(uuid).also { OfflineInfo.cache.add(it) }
        } catch (e: Exception) {
            null
        }
    fun findOfflineInfoByName(name: String) = OfflineInfo.findByName(name)

    fun createOfflinePlayer(uuid: UUID) = findOfflineInfoByPlayer(uuid)
        ?: OfflineInfo(uuid, true).also { OfflineInfo.cache.add(it) }

    fun forEachPlayer(l: (Info) -> Unit) = Info.cache.forEach(l)
    fun forEachChatter(l: (ChatInfo) -> Unit) {
        Info.cache.forEach(l)
        chatters.forEach(l)
    }

    fun forEachOffline(l: (OfflineInfo) -> Unit) = OfflineInfo.cache.forEach(l)

    fun addOffline(info: OfflineInfo) = OfflineInfo.cache.add(info)
    fun add(info: Info) = Info.cache.add(info)
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
    val cache = HashMap<String, String>()
    PlayerManager.forEachChatter {
        if (it is Info && !it.isLogin) return@forEachChatter
        val lang = it.targetLang
        if (!cache.containsKey(lang)) {
            cache[lang] = TextUtil.getColoredText(
                Language.got(lang, value, replaceWith), color,
                bold = false,
                underlined = false
            )
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
            it.playerOutputStream.send(cache[lang]!!)
        }
    }
}