package com.zhufu.opencraft

import com.zhufu.opencraft.util.toErrorMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerMoveEvent
import kotlin.math.abs

object Listener: Listener {
    private val Material.isOre: Boolean
            get() = this.isBlock && this.name.contains("ORE")
    private fun findPlayer(player: Player) = PrevHacking.playerList.firstOrNull { it.player == player }

    @EventHandler
    fun onPlayerBreakBlock(event: BlockBreakEvent) {
        if (event.player.world != Base.surviveWorld)
            return
        val player = findPlayer(event.player) ?: return
        if (!event.block.type.isOre) {
            if (player.lastBlockBroken.world == event.player.world && player.lastOrePointed != null && player.lastOrePointed!!.world == player.lastBlockBroken.world) {
                val closer = player.lastBlockBroken.distance(player.lastOrePointed!!) > event.block.location.distance(player.lastOrePointed!!) && event.block.location.distance(player.lastOrePointed!!) <= 5
                val distance = abs(player.lastBlockBroken.y - event.block.location.y) >= 2
                if (distance) {
                    if (closer) {
                        player.setIsHacking(player.isHacking + 6)
                    } else {
                        player.hasBrokenBlockFarther = true
                    }
                    player.lastBlockBroken = event.block.location
                }
                if (closer){
                    player.blockToBeFilled.add(event.block)
                }
            }
            else{
                player.lastBlockBroken = event.block.location
            }
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent){
        val player = findPlayer(event.player) ?: return
        val targetBlock = try{ event.player.getTargetBlock(setOf(Material.AIR, Material.DIRT, Material.GRASS, Material.STONE), 15) } catch (igor: Exception){ return }
        if (targetBlock.type.isOre && targetBlock.location != player.lastOrePointed) {
            player.lastOrePointed = targetBlock.location
            if (PrevHacking.showPlayerOrePointInfo)
                println("[PrevHacking] ${event.player.name} pointed to an ore!")
        }
    }
}