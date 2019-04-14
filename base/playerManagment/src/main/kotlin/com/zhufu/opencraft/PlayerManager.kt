package com.zhufu.opencraft

import com.zhufu.opencraft.Info.Companion.mList
import com.zhufu.opencraft.OfflineInfo.Companion.offlineList
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.special_items.SpecialItem
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.collections.ArrayList

object PlayerManager : Listener, PluginBase {
    private val chatters = ArrayList<ChatInfo>()
    private lateinit var plugin: JavaPlugin
    val isInit: Boolean
        get() = ::plugin.isInitialized

    fun init(plugin: JavaPlugin) {
        PlayerManager.plugin = plugin
        Info.plugin = plugin
    }

    fun removeChatter(chatInfo: ChatInfo) = chatters.remove(chatInfo)
    fun findChatter(name: String) = chatters.firstOrNull { it.id == name }
    fun removeFirstChatter(l: (ChatInfo) -> Boolean): Boolean {
        val index = chatters.indexOfFirst(l)
        if (index == -1)
            return false
        chatters.removeAt(index)
        return true
    }

    fun findInfoByPlayer(player: Player) = Info.findByPlayer(player)
    fun findInfoByPlayer(uuid: UUID) = Info.findByPlayer(uuid)
    fun findOfflinePlayer(uuid: UUID): OfflineInfo? = mList.firstOrNull { it.uuid == uuid }
            ?: offlineList.firstOrNull { it.uuid == uuid }
            ?: try {
                OfflineInfo(uuid).also { offlineList.add(it) }
            } catch (e: Exception) {
                null
            }
    fun createOfflinePlayer(uuid: UUID) = findOfflinePlayer(uuid)
            ?: OfflineInfo(uuid, true).also { offlineList.add(it) }

    fun forEachPlayer(l: (Info) -> Unit) = mList.forEach(l)
    fun forEachChatter(l: (ChatInfo) -> Unit) {
        mList.forEach(l)
        chatters.forEach(l)
    }
    fun forEachOffline(l: (OfflineInfo) -> Unit) = offlineList.forEach(l)

    fun addOffline(info: OfflineInfo) = offlineList.add(info)
    fun add(info: Info) = mList.add(info)
    fun remove(p: Player) = forEachPlayer {
        if (it.uuid == p.uniqueId){
            it.destroy()
        }
    }

    fun onPlayerOutOfDemo(info: Info){
        val event = PlayerTeleportedEvent(info.player, info.player.location, Base.lobby.spawnLocation)
        Bukkit.getPluginManager().callEvent(event)
        if (!event.isCancelled) {
            info.player.teleport(Base.lobby.spawnLocation)
            info.status = Info.GameStatus.InLobby
        }
        showPlayerOutOfDemoTitle(info.player)
    }
    fun showPlayerOutOfDemoTitle(player: Player){
        val getter = player.lang()
        player.sendTitle(TextUtil.info(getter["survey.title"]), TextUtil.tip(getter["survey.toBeMember"]),7,80,7)
    }
    val Inventory.containsSpecialItem: Boolean
        get() = this.any { if (it != null) SpecialItem.isSpecial(it) else false }
    val Inventory.specialItems: List<SpecialItem>
        get() {
            val r = ArrayList<SpecialItem>()
            for (i in 0 until this.size){
                val it = this.getItem(i)?:continue
                val getter = viewers.firstOrNull()?.lang()?:return emptyList()
                SpecialItem.getByItem(it,getter)?.apply {
                    inventoryPosition = i
                    r.add(this)
                }
            }
            return r
        }
}