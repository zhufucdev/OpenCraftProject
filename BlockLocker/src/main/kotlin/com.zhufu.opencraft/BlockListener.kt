package com.zhufu.opencraft

import com.zhufu.opencraft.Base.tradeWorld
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerMoveEvent
import java.util.*

object BlockListener : Listener {
    @EventHandler
    fun onBreakBlock(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player
        val info = BlockLockManager[block.location]?:return
        if (info.world != event.player.location.world!!.name) return
        if (!info.canAccess(player)){
            event.player.error(getLang(event.player,"block.error.inaccessible"))
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockPlaced(event: BlockPlaceEvent){
        val block = event.block
        val player = event.player
        val info: BlockLockManager.BaseInfo = BlockLockManager[block.location]?:return
        if (!info.canAccess(player)){
            event.player.error(getLang(event.player,"block.error.inaccessible"))
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockExplode(event: EntityExplodeEvent){
        if (event.blockList().any { BlockLockManager[it.location] != null }){
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent){
        val l = event.to
        val info = BlockLockManager[l!!]
        val playerInfo = PlayerManager.findInfoByPlayer(event.player)?:return

        if (info != null && info.name != "Lobby"){
            fun groupOrBlock(info: BlockLockManager.BaseInfo,getter: Language.LangGetter): String {
                return if (info is BlockLockManager.GroupBlockInfo) {
                    getter["block.group"]
                } else {
                    getter["block.block"]
                }
            }

            if (!playerInfo.tag.getBoolean("isInChunk",false)) {
                Bukkit.getPluginManager().callEvent(PlayerGotoTerritoryEvent(event.player, info))
                playerInfo.tag.set("isInChunk",true)

                if (info.owner != event.player.uniqueId.toString()) {
                    //Notice the player who moved.
                    getLangGetter(event.player.info()).let {
                        event.player.sendActionText(
                                TextUtil.info(
                                        it["block.tip.enter",
                                                info.name,
                                                groupOrBlock(info,it),
                                                if (info.canAccess(event.player))
                                                    it["block.tip.accessible"]
                                                else
                                                    it["block.tip.inaccessible"]
                                        ]
                                )
                        )
                    }
                    Bukkit.getPlayer(UUID.fromString(info.owner))?.apply {
                        info(getLang(this,"block.tip.oneEnter",event.player.name,info.name))
                    }
                }

                val map = info.parent?.accessMap?:info.accessMap
                map[Date()] = event.player.uniqueId
                if (map.size > 100){
                    for (i in 0 .. map.size - 100){
                        map.remove(map.keys.elementAt(i))
                    }
                }
                if (info.parent != null){
                    info.parent!!.accessMap = map
                } else {
                    info.accessMap = map
                }
            }
        } else if (info == null && playerInfo.tag.getBoolean("isInChunk",false)){
            playerInfo.tag.set("isInChunk",false)
        }
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent){
        if((event.inventory.location?:return).world == tradeWorld) return
        val info = BlockLockManager[event.inventory.location!!]?:return
        if (!info.canAccess(event.player.uniqueId)){
            event.isCancelled = true
            event.player.error(getLang(event.player,"block.error.inaccessible"))
        }
    }
}