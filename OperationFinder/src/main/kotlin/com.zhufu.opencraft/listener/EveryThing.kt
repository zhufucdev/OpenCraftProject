package com.zhufu.opencraft.listener

import com.zhufu.opencraft.OperationChecker
import com.zhufu.opencraft.operations.BlockOperationType
import com.zhufu.opencraft.operations.PlayerBlockOperation
import com.zhufu.opencraft.operations.PlayerMoveOperation
import com.zhufu.opencraft.operations.PlayerOpenInventoryOperation
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class EveryThing(val plugin: JavaPlugin) : Listener {
    private var monitorTask: BukkitTask? = null
    fun startMonitoring(period: Long) {
        monitorTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            monitorOnce()
        }, 0L, period)
    }

    fun stopMonitoring() {
        monitorTask?.cancel()
    }

    private fun monitorOnce() {
        plugin.server.onlinePlayers.forEach {
            OperationChecker.append(PlayerMoveOperation(it.name, System.currentTimeMillis(), it.location))
        }
    }

    @EventHandler
    fun onPlayerBreakBlock(event: BlockBreakEvent) {
        OperationChecker.append(
            PlayerBlockOperation(
                event.player.name,
                System.currentTimeMillis(),
                event.block.type, event.block.location,
                BlockOperationType.BREAK
            )
        )
    }

    @EventHandler
    fun onPlayerPlaceBlock(event: BlockPlaceEvent) {
        OperationChecker.append(
            PlayerBlockOperation(
                event.player.name,
                System.currentTimeMillis(),
                event.block.type, event.block.location,
                BlockOperationType.PLACE
            )
        )
    }

    @EventHandler
    fun onPlayerOpenInventory(event: InventoryOpenEvent) {
        OperationChecker.append(
            PlayerOpenInventoryOperation(
                event.player.name,
                System.currentTimeMillis(),
                event.inventory
            )
        )
    }
}