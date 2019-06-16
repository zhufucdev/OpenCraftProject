package com.zhufu.opencraft.headers.server_wrap

import org.bukkit.Bukkit
import java.nio.file.Paths
import java.util.function.Function
@Suppress("unused")
class ServerSelf {
    val dataFolder = Paths.get("plugins","ServerCore").toFile()!!
    val onServerPing = arrayListOf<Function<Array<SimpleServerListPingEvent>,Any?>>()
    val onServerBoot = arrayListOf<Function<Any?,Any?>>()
    val name = Bukkit.getName()
    val version = Bukkit.getVersion()
    val bukkitVersion = Bukkit.getBukkitVersion()
}