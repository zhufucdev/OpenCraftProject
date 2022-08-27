package com.zhufu.opencraft

import com.zhufu.opencraft.data.ServerPlayer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.util.Vector
import java.util.*

class TradeTerritoryInfo {
    val x: Int
    val z: Int

    /**
     * [fromX] is always smaller than [toX]
     */
    val fromX: Int
    val fromZ: Int
    val toX: Int
    val toZ: Int

    val center: Location
        get() {
            val dest = Location(Base.tradeWorld, fromX + 16.0, 60.0, fromZ + 16.0)
            while (dest.blockY < Base.tradeWorld.maxHeight
                && (dest.block.type != Material.AIR || dest.clone().add(Vector(0, 1, 0)
                ).block.type != Material.AIR)
            ) {
                dest.add(Vector(0, 1, 0))
            }
            return dest
        }

    fun contains(location: Location) = location.blockX in fromX..toX && location.blockZ in fromZ..toZ

    constructor(id: Int) {
        with(Base.getUniquePair(id)) {
            this@TradeTerritoryInfo.x = first
            this@TradeTerritoryInfo.z = second
        }

        fromX = 48 * (x - 1) + 16
        fromZ = 48 * (z - 1) + 16
        toX = fromX + 32
        toZ = fromZ + 32
    }

    constructor(info: ServerPlayer) : this(info.territoryID)
}