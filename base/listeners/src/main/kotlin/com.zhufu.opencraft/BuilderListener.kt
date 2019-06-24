package com.zhufu.opencraft

import org.bukkit.*
import org.bukkit.entity.HumanEntity
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

    fun isInBuilderMode(player: HumanEntity) = player.info()?.isBuilder == true && player.gameMode == GameMode.CREATIVE

    fun updatePlayerLevel(player: OfflinePlayer, lvl: Int) {
        val online = player.player
        if (lvl == 0 && online != null && isInBuilderMode(online)) {
            switch(online)
        }
        player.offlineInfo()?.builderLevel = lvl
        updatePermissions(online ?: return, lvl)
    }

    private fun updatePermissions(player: Player, lvl: Int) {
        val we = Bukkit.getPluginManager().getPlugin("WorldEdit")
        if (we != null) {
            val per = player.addAttachment(we) ?: return
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
        if (player.gameMode == GameMode.SURVIVAL) {
            info.status = Info.GameStatus.Building
            info.inventory.create("builder").load()
            player.gameMode = GameMode.CREATIVE
            updatePermissions(player, info.builderLevel)
        } else {
            info.status = Info.GameStatus.Surviving
            info.inventory.create("survivor").load()
            player.gameMode = GameMode.SURVIVAL
            updatePermissions(player, 0)
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    fun onPlayerPlaceBlock(event: BlockPlaceEvent) {
        if (isInBuilderMode(event.player)) {
            val lvl = event.player.info()?.builderLevel
            if (lvl != null && lvl > 2 && isBlockLimit(event.block.type)) {
                event.isCancelled = true
                event.player.sendMessage(TextUtil.error(Language[event.player, "builder.error.block"]))
            } else if (!blocks.contains(event.block.location))
                blocks.add(event.block.location)
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    fun onPlayerBreakBlock(event: BlockBreakEvent) {
        if (blocks.contains(event.block.location)) {
            if (isInBuilderMode(event.player)) {
                blocks.remove(event.block.location)
            } else if (!event.player.isOp) {
                event.isCancelled = true
                event.player.sendMessage(TextUtil.error(Language[event.player, "builder.error.placeBlock"]))
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
        if (isInBuilderMode(event.player) && event.player.info()?.builderLevel!! > 1) {
            event.isCancelled = true
            event.player.sendMessage(TextUtil.error(Language[event.player, "builder.error.dropItem"]))
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerOpenInventory(event: InventoryOpenEvent) {
        if (isInBuilderMode(event.player) && event.inventory.type != InventoryType.CREATIVE && event.player.info()?.builderLevel!! > 1) {
            event.isCancelled = true
            event.player.sendMessage(TextUtil.error(Language[event.player, "builder.error.inventory"]))
        }
    }

    fun isBlockLimit(type: Material) = mPlugin!!.config.getBoolean("limitsOfBlock.${type.name.toLowerCase()}", false)
    fun init(plugin: Plugin) {
        mPlugin = plugin

        plugin.config.getList("buildersBlock")!!.forEach {
            if (it != null)
                blocks.add(it as Location)
        }

        if (!plugin.config.isSet("limitsOfBlock")) {
            val conf = plugin.config.createSection("limitsOfBlock")
            conf.set("tnt", true)
            conf.set("redstone", true)
            plugin.config.set("limitsOfBlock", conf)
        }

        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun onServerClose() {
        mPlugin!!.config.set("buildersBlock", blocks.toList())
        mPlugin!!.saveConfig()
    }
}