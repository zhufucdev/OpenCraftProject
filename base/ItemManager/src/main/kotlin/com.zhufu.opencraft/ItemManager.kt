package com.zhufu.opencraft

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.plugin.java.JavaPlugin

class ItemManager : JavaPlugin(), Listener {
    companion object {
        const val customName = "§6即将被清除"
    }

    private val toBeCleaned: List<World?>
        get() = listOf(
            Base.surviveWorld,
            Base.tradeWorld,
            Bukkit.getWorld("world_nether"),
            Bukkit.getWorld("world_the_end")
        )
    var delay: Long
        get() = config.getLong("cleanDelay", 5 * 20 * 60)
        set(value) = config.set("cleanDelay", value)

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        fun clean(entity: Entity) {
            Bukkit.getScheduler().runTaskLater(this, { _ ->
                if (event.itemDrop == null)
                    return@runTaskLater
                entity.remove()
            }, 5 * 20)
        }

        Bukkit.getScheduler().runTaskLater(this, { _ ->
            if (event.itemDrop == null)
                return@runTaskLater
            event.itemDrop.customName = "${ChatColor.GOLD}即将被清除"
            event.itemDrop.isCustomNameVisible = true
            clean(event.itemDrop)
        }, delay)
    }

    private fun toCleanOrNot(entity: Entity): Boolean {
        val type = entity.type
        return (type == EntityType.DROPPED_ITEM || type == EntityType.CREEPER || type == EntityType.ZOMBIE || type == EntityType.ZOMBIE
                || type == EntityType.SKELETON || type == EntityType.ENDERMAN || type == EntityType.HUSK || type == EntityType.SPIDER
                || type == EntityType.CAVE_SPIDER)
        && !entity.hasMetadata("NPC")
    }

    private fun doCleaning() {
        toBeCleaned.forEach {
            it?.entities?.forEach { entity ->
                if (toCleanOrNot(entity)) {
                    var distance = server.onlinePlayers.first().location.distance(entity.location)
                    server.onlinePlayers.forEach { p ->
                        if (p.world == entity.world) {
                            val t = p.location.distance(entity.location)
                            if (t < distance) {
                                distance = t
                            }
                        }
                    }
                    if (distance >= 100) {
                        entity.customName = customName
                        entity.isCustomNameVisible = true
                    }
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(this, { _ ->
            toBeCleaned.forEach {
                it?.entities?.forEach { entity ->
                    if (entity.customName == customName) {
                        entity.remove()
                    }
                }
            }
        }, 5 * 20)
    }

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "im") {
            if (!sender.isOp) {
                sender.sendMessage("${ChatColor.RED}您没有权限使用该命令")
                return true
            }
            if (args.isEmpty()) {
                sender.sendMessage("${ChatColor.RED}用法错误")
                return true
            }
            when (args.first()) {
                "delay" -> {
                    if (args.size == 1) {
                        sender.sendMessage("当前清除物品的时间间隔为${delay}(约${delay / 20}秒)")
                    } else {
                        val num = args[1].toLongOrNull()
                        if (num == null) {
                            sender.sendMessage("${ChatColor.RED}非法参数 ${args[1]}: 不是自然数")
                            return true
                        }
                        delay = num
                        sender.sendMessage("${ChatColor.YELLOW}已将清除物品的时间间隔设置为${args[1]}(约${num / 20}秒)")
                        return true
                    }
                }
                "clean" -> {
                    doCleaning()
                    return true
                }
            }
        }
        return false
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (command.name == "im" && sender.isOp) {
            val commands = mutableListOf("delay", "clean")
            if (args.size == 1) {
                return if (args.first().isNotEmpty()) {
                    val r = ArrayList<String>()
                    commands.forEach { if (it.startsWith(args.first())) r.add(it) }
                    r
                } else {
                    commands
                }
            }
        }
        return mutableListOf()
    }
}