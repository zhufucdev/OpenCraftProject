package com.zhufu.opencraft

import org.bukkit.configuration.file.YamlConfiguration

object Game {
    const val gameWarn = "gameWarn"
    var env = YamlConfiguration()
    val varNames = arrayOf(
        "reloadDelay",
        "inventorySaveDelay",
        "prisePerBlock",
        "backToDeathPrise",
        "countOfSurveyQuestion",
        "secondsPerQuestion",
        "notice",
        "url"
    )
}