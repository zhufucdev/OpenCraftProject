package com.zhufu.opencraft.data

import com.zhufu.opencraft.api.ChatInfo
import com.zhufu.opencraft.api.ServerCaller
import com.zhufu.opencraft.player_community.PlayerOutputStream
import com.zhufu.opencraft.util.WebOutputStream
import org.bukkit.configuration.ConfigurationSection
import java.io.File
import java.net.InetAddress
import java.util.*

abstract class WebInfo(createNew: Boolean = false, uuid: UUID, nameToExtend: String? = null) :
    ServerPlayer(createNew, uuid, nameToExtend), ChatInfo {
    abstract val face: File
    override var doNotTranslate: Boolean = false
    override val displayName: String
        get() = "${name ?: "unknown"} Web"
    override val targetLang: String
        get() = userLanguage

    private var exchanges = 0
    private var streamsReturned = 0
    override val playerOutputStream: PlayerOutputStream
        get() {
            streamsReturned++

            val returned = streamsReturned
            var i = 0
            while (returned != exchanges) {
                Thread.sleep(200)
                if (streamsReturned - exchanges
                    >= (ServerCaller["GetWebConfig"]!!(listOf())
                            as ConfigurationSection).getInt("chatPackLossThreshold")
                    || exchanges > returned
                    || i >= 25
                ) {
                    streamsReturned = 1
                    exchanges = 1
                    break
                }
                i++
            }
            return WebOutputStream(this)
        }

    companion object {
        val users = HashMap<InetAddress, WebInfo>()

        fun of(uuid: UUID) = if (RegisteredInfo.exists(uuid)) {
            RegisteredInfo(uuid)
        } else {
            PreregisteredInfo(uuid)
        }
    }
}