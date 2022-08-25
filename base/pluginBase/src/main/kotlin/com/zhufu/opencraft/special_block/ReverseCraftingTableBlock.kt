package com.zhufu.opencraft.special_block

import com.zhufu.opencraft.api.ServerCaller
import com.zhufu.opencraft.error
import com.zhufu.opencraft.getter
import com.zhufu.opencraft.special_item.ReverseCraftingTable
import com.zhufu.opencraft.util.Language
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.util.*

class ReverseCraftingTableBlock(location: Location) : StatelessSpecialBlock(location, SBID), Dropable {
    override val itemToDrop: ItemStack
        get() = ReverseCraftingTable(Language.LangGetter.default)

    override val eventListener = object : Listener {
        @EventHandler
        fun onPlayerRightClick(event: PlayerInteractEvent) {
            if (event.action != Action.RIGHT_CLICK_BLOCK || event.clickedBlock?.isThis() != true) {
                return
            }
            event.isCancelled = true
            ServerCaller["SolveRCT"]?.invoke(listOf(event.player))
                ?: event.player.error(event.player.getter()["rct.error.unavailable"])
        }
    }

    companion object : SBCompanion {
        override fun from(location: Location) = ReverseCraftingTableBlock(location)
        override val material: Material
            get() = Material.SMITHING_TABLE
        override val SBID: UUID
            get() = UUID.fromString("BE2204E6-87D6-4D9D-8C5F-8C2F8EC4FA5C")
    }
}