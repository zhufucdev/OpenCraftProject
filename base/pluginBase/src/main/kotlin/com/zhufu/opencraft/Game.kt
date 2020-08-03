package com.zhufu.opencraft

import org.bukkit.configuration.file.YamlConfiguration
import kotlin.math.sqrt

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
        "url",
        "diamondExchange",
        "debug",
        "ssHotReload",
        "survivalCenter",
        "lobbyRadius"
    )

    val chart: List<ServerPlayer>
        get() {
            val b = arrayListOf<Pair<ServerPlayer, Double>>()
            ServerPlayer.forEachSaved {
                if (!it.isOp) {
                    var weight =
                        sqrt(it.currency.toDouble()) - 1200000.0 /   (it.gameTime / 1000.0 + 400) + 3000 + sqrt(it.damageDone)
                    if (it.isSurveyPassed) {
                        weight += 40
                    }
                    if (it.isBuilder) {
                        weight *= 1.2
                    }
                    b.add(it to weight)
                }
            }
            b.sortBy { it.second }

            val r = arrayListOf<ServerPlayer>()
            for (i in b.lastIndex downTo 0) {
                r.add(b[i].first)
            }
            return r
        }
    val dailyChart: List<ServerPlayer>
        get() {
            val b = arrayListOf<Pair<ServerPlayer, Double>>()
            ServerPlayer.forEachSaved {
                if (!it.isOp) {
                    val static = it.statics

                    var weight = 0.0
                    if (static != null) {
                        weight =
                            static.damageToday * 0.2 + static.currencyDelta * 0.2 + static.timeToday * 0.1
                    }
                    if (it.isSurveyPassed) {
                        weight += 10
                    }
                    if (it.isBuilder) {
                        weight *= 1.2
                    }
                    b.add(it to weight)
                }
            }
            b.sortBy { it.second }

            val r = arrayListOf<ServerPlayer>()
            for (i in b.lastIndex downTo 0) {
                r.add(b[i].first)
            }
            return r
        }
}