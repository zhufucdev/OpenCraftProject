package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.PlayerModifier
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Objective

interface Tickable {
    /**
     * Called each tick if the item is in a player's inventory.
     */
    fun doPerTick(mod: PlayerModifier, contract: YamlConfiguration, score: Objective, scoreboardSorter: Int)
}