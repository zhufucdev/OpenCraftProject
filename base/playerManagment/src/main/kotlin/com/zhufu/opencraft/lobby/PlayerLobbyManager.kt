package com.zhufu.opencraft.lobby

import com.zhufu.opencraft.Info
import com.zhufu.opencraft.OfflineInfo
import org.bukkit.entity.Player

object PlayerLobbyManager {
    private val mList = arrayListOf<PlayerLobby>()
    fun init() {
        OfflineInfo.forEach {
            mList.add(PlayerLobby(it))
        }
    }
    fun saveAll(){
        mList.forEach {
            it.save()
        }
    }
    operator fun get(owner: OfflineInfo): PlayerLobby {
        val index = mList.indexOfFirst { it.owner == owner }
        if (index != -1)
            return mList[index]
        val r = PlayerLobby(owner)
        mList.add(r)
        return r
    }
    fun list() = mList.toList()
    val targetMap = HashMap<Player, PlayerLobby>()
    fun targetOf(player: Player) = targetMap[player]
    fun isTargetOf(player: Player) = targetMap[player]?.contains(player.location) == true
    fun isInOwnLobby(info: Info) = get(info).contains(info.player.location)
}