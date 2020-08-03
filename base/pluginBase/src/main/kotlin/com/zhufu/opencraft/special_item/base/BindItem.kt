package com.zhufu.opencraft.special_item.base

import com.zhufu.opencraft.*
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import java.util.*

abstract class BindItem : SpecialItem() {
    protected lateinit var owner: ServerPlayer

    override fun onCreate(owner: Player, vararg args: Any) {
        super.onCreate(owner, *args)
        this.owner = owner.info() ?: error("Player doesn't have an info.")
    }

    override fun onSaveStatus(config: ConfigurationSection) {
        super.onSaveStatus(config)
        config.set("owner", owner.uuid.toString())
    }

    override fun onRestore(savedStatus: ConfigurationSection) {
        super.onRestore(savedStatus)
        owner = OfflineInfo.findByUUID(UUID.fromString(savedStatus.getString("owner")))!!
            .let { if (it.isOnline) it.onlinePlayerInfo!! else it }
    }

    override fun onDrop(event: PlayerDropItemEvent) {
        super.onDrop(event)
        event.isCancelled = true
    }

    override fun onClick(event: InventoryClickEvent) {
        super.onClick(event)
        if (event.inventory.holder != event.whoClicked) {
            event.isCancelled = true
        }
    }
}