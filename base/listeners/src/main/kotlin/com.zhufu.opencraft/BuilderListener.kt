package com.zhufu.opencraft

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.plugin.Plugin

object BuilderListener : Listener {
    private var mPlugin: Plugin? = null
    private val blocks = ArrayList<Location>()

    fun updatePlayerLevel(player: OfflinePlayer, lvl: Int) {
        val online = player.player
        if (lvl == 0 && online != null && online.info()?.isInBuilderMode == true) {
            switch(online)
        }
        player.offlineInfo()?.builderLevel = lvl
        updatePermissions(online ?: return, lvl)
    }

    private fun updatePermissions(player: Player, lvl: Int) {
        val we = Bukkit.getPluginManager().getPlugin("WorldEdit")
        if (we != null) {
            val per = player.addAttachment(we)
            when (lvl) {
                1 -> {
                    per.setPermission("worldedit.*", true)
                }
                else -> {
                    per.setPermission("worldedit.*", false)
                }
            }
        }
    }

    fun switch(player: Player) {
        val info = player.info()
        if (info == null) {
            Language.getDefault("player.error.unknown")
            return
        }
        if (info.status == Info.GameStatus.Surviving) {
            info.status = Info.GameStatus.Building
            info.inventory.create("builder").apply {
                set("gameMode", GameMode.CREATIVE.name)
                load()
            }
            updatePermissions(player, info.builderLevel)
        } else if (info.status == Info.GameStatus.Building) {
            info.status = Info.GameStatus.Surviving
            info.inventory.create("survivor").load()
            player.gameMode = GameMode.SURVIVAL
            updatePermissions(player, 0)
        }
    }

    @EventHandler
    fun onPlayerPlaceBlock(event: BlockPlaceEvent) {
        if (event.player.info()?.isInBuilderMode == true) {
            val lvl = event.player.info()?.builderLevel
            if (lvl != null && lvl > 2 && isBlockLimit(event.block.type)) {
                event.isCancelled = true
                event.player.sendMessage(TextUtil.error(Language[event.player, "builder.error.block"]))
            } else if (!blocks.contains(event.block.location)) {
                blocks.add(event.block.location)
            }
        }
    }

    @EventHandler
    fun onPlayerBreakBlock(event: BlockBreakEvent) {
        if (blocks.contains(event.block.location)) {
            if (event.player.info()?.isInBuilderMode == true) {
                blocks.remove(event.block.location)
            } else if (!event.player.isOp) {
                event.isCancelled = true
                event.player.sendMessage(TextUtil.error(Language[event.player, "builder.error.breakBlock"]))
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockExplode(event: EntityExplodeEvent) {
        val a = event.blockList().count { blocks.contains(it.location) }
        if (a * 2 > event.blockList().size) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (event.player.info()?.isInBuilderMode == true && event.player.info()?.builderLevel!! > 1) {
            event.isCancelled = true
            event.player.sendMessage(TextUtil.error(Language[event.player, "builder.error.dropItem"]))
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerOpenInventory(event: InventoryOpenEvent) {
        if (event.player.info()?.isInBuilderMode == true && event.inventory.type != InventoryType.CREATIVE && event.player.info()?.builderLevel!! > 1) {
            event.isCancelled = true
            event.player.sendMessage(TextUtil.error(Language[event.player, "builder.error.inventory"]))
        }
    }

    fun isBlockLimit(type: Material) =
        mPlugin!!.config.getStringList("limitsOfBlock").any {
            Regex(it).matches(type.name.toLowerCase())
        }
    fun init(plugin: Plugin) {
        mPlugin = plugin

        plugin.config.getList("buildersBlock")!!.forEach {
            if (it != null)
                blocks.add(it as Location)
        }

        if (!plugin.config.isSet("limitsOfBlock")) {
            plugin.config.set("limitsOfBlock", listOf("tnt", "redstone", "ender_chest"))
        }

        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun saveConfig() {
        mPlugin!!.config.set("buildersBlock", blocks.toList())
        mPlugin!!.saveConfig()
    }
}