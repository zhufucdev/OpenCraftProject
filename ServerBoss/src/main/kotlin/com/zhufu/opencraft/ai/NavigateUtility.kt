package com.zhufu.opencraft.ai

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.abs

object NavigateUtility {
    fun targetFlyable(npc: Entity, target: Player) {
        npc.setGravity(false)
        val subtract = target.location.y - npc.location.y
        if (abs(subtract) >= 1.0) {
            npc.velocity = Vector(0.0, subtract * 0.2, 0.0)
        }
    }

    fun dashTo(npc: Entity, to: Location, rate: Double) {
        val direction = to.toVector().subtract(npc.location.toVector()).normalize()
        npc.velocity = direction.multiply(rate)
    }
}