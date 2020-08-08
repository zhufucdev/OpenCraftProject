package com.zhufu.opencraft.special_item.base.locate

import com.zhufu.opencraft.info
import com.zhufu.opencraft.special_item.base.SpecialItem
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

class PlayerLocation(val player: Player, val itemID: UUID) : ItemLocation() {
    private val inventory = player.info()?.inventory?.present?.name
    private fun checkOnline(): Player = player.player ?: error("Player ${player.name} isn't available.")

    val position: Int
        get() {
            val player = checkOnline()
            val r =
                player.inventory.indexOfFirst { SpecialItem.getSIID(it) == itemID }
            if (r == -1)
                error("${player.name} doesn't have an item with ID $itemID.")
            return r
        }

    override val isAvailable: Boolean
        get() = player.player?.inventory?.any { SpecialItem.getSIID(it) == itemID } == true

    private var lastItem: ItemStack = player.inventory.getItem(position)!!
    override val itemStack: ItemStack
        get() =
            if (player.isOnline) {
                if (inventory != player.info()?.inventory?.present?.name)
                    lastItem
                else
                    player.inventory.getItem(position)!!.also { lastItem = it }
            } else
                lastItem

    override fun push() {
        val item = lastItem
        checkOnline().inventory.setItem(position, item)
    }

    override fun toString(): String = "PlayerLocation{ " +
            "player = ${player.name}, " +
            "expectedItemID = $itemID " +
            "}"

    override fun serialize() = mutableMapOf("player" to player.uniqueId.toString(), "siid" to itemID.toString())

    companion object {
        @JvmStatic
        fun deserialize(d: Map<String, Any>) =
            PlayerLocation(
                Bukkit.getPlayer(UUID.fromString(d["player"] as String)) ?: error("Player ${d["player"]} is offline."),
                UUID.fromString(d["siid"] as String)
            )
    }
}