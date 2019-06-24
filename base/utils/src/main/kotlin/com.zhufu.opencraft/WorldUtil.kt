package com.zhufu.opencraft

import org.bukkit.Difficulty
import org.bukkit.GameRule
import org.bukkit.World

fun World.peace() {
    setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false)
    setGameRule(GameRule.DO_WEATHER_CYCLE,false)
    setGameRule(GameRule.DO_MOB_SPAWNING,false)
    time = 5000
    pvp = false
    difficulty = Difficulty.PEACEFUL
    livingEntities.forEach { it.remove() }
}