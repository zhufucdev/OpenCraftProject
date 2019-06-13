package com.zhufu.opencraft.headers

import com.zhufu.opencraft.Header
import com.zhufu.opencraft.Info
import com.zhufu.opencraft.OfflineInfo
import com.zhufu.opencraft.headers.util.LocationUtils
import com.zhufu.opencraft.headers.util.PlayerUtils
import com.zhufu.opencraft.headers.util.UUIDUtils
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.function.Function

object ServerHeader : Header {
    override val members: List<Pair<String, Any?>>
        get() = listOf(
            "info" to Info.Companion,
            "offlineInfo" to OfflineInfo.Companion,
            "UUID" to UUIDUtils,
            "location" to LocationUtils,
            "player" to PlayerUtils,
            "Player" to Function<String, Player?> {
                PlayerUtils.findByName(it)
            },
            "OfflinePlayer" to Function<String, OfflinePlayer?> {
                PlayerUtils.findOfflineByName(it)
            }
        )
}