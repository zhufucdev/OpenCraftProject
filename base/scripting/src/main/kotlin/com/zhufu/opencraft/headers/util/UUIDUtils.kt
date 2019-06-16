package com.zhufu.opencraft.headers.util

import org.bukkit.Bukkit
import java.util.*

@Suppress("unused")
object UUIDUtils {
    fun fromString(string: String) = UUID.fromString(string)!!
    fun random() = UUID.randomUUID()!!
    fun getPlayerUUID(name: String) = Bukkit.getOfflinePlayer(name).uniqueId
}