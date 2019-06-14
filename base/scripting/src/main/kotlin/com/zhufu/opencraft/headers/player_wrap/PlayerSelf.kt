package com.zhufu.opencraft.headers.player_wrap

import com.google.common.cache.CacheBuilder
import com.zhufu.opencraft.*
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent

class PlayerSelf private constructor(private val serverInfo: ServerPlayer,private val executor: Scripting.Executor) {
    companion object {
        val cache = CacheBuilder.newBuilder().maximumSize(50).build<ServerPlayer, PlayerSelf>()!!
        fun from(src: ServerPlayer,executor: Scripting.Executor) = cache.get(src) {
            PlayerSelf(src,executor)
        }!!
    }

    val getter = Language.LangGetter(serverInfo.userLanguage)
    // Basis Info
    val name get() = serverInfo.name!!
    val nickname get() = serverInfo.nickname
    fun isLogin() = if (serverInfo is Info) serverInfo.isLogin else false
    fun isRegistered() = serverInfo.password != null
    fun getUUID() = serverInfo.uuid
    fun isOp() = serverInfo.isOp
    // Data & Statics
    val gameTime get() = serverInfo.gameTime
    var language
        get() = serverInfo.userLanguage
        set(value) {
            serverInfo.userLanguage = value
        }
    val state get() = (if (serverInfo is Info) serverInfo.status else Info.GameStatus.Offline).name
    //val playerDir get() = serverInfo.playerDir
    val gameTimeToday get() = serverInfo.statics?.timeToday ?: 0
    // => Physical Info
    private fun <T> ease(l: Player.() -> T): T? = if (serverInfo is Info) l(serverInfo.player) else null

    val inventory get() = PlayerInventory.of(serverInfo)
    val location get() = ease { location }
    val gameMode get() = ease { gameMode.name.toLowerCase() }
    var yaw: Double?
        get() = ease { location.yaw.toDouble() }
        set(value) {
            if (value != null) {
                if (serverInfo is Info) runSync {
                    serverInfo.player.teleport(
                        serverInfo.player.location.apply { yaw = value.toFloat() },
                        PlayerTeleportEvent.TeleportCause.COMMAND
                    )
                } else throw IllegalStateException(getter["scripting.error.offline"])
            } else {
                throw IllegalArgumentException(getter["scripting.error.nullValue", "Yaw"])
            }
        }
    var pitch: Double?
        get() = ease { location.pitch.toDouble() }
        set(value) {
            if (value != null) {
                if (serverInfo is Info) runSync {
                    serverInfo.player.teleport(
                        serverInfo.player.location.apply { pitch = value.toFloat() },
                        PlayerTeleportEvent.TeleportCause.COMMAND
                    )
                } else throw IllegalStateException(getter["scripting.error.offline"])
            } else {
                throw IllegalArgumentException(getter["scripting.error.nullValue", "Pitch"])
            }
        }

    fun setYawAndPitch(yaw: Double, pitch: Double) {
        runSync {
            ease {
                teleport(
                    location.apply {
                        this.yaw = yaw.toFloat()
                        this.pitch = pitch.toFloat()
                    }
                )
            }
        }
    }

    val level get() = ease { level }
    val foodLevel get() = ease { foodLevel }
    val spawnpoint get() = serverInfo.tag.getSerializable("surviveSpawn",Location::class.java,null)
    val currency get() = serverInfo.currency
    val info
        get() = if (executor == Scripting.Executor.Operator) {
            serverInfo
        } else {
            throw IllegalAccessError(getter["command.error.permission"])
        }
    val player
        get() = if (executor == Scripting.Executor.Operator) {
            if (serverInfo is Info)
                serverInfo.player
            else
                null
        } else {
            throw IllegalAccessError(getter["command.error.permission"])
        }
    val maxLoopExecution get() = serverInfo.maxLoopExecution
}