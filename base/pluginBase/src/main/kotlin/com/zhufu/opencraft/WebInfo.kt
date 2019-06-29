package com.zhufu.opencraft

import com.sun.net.httpserver.HttpExchange
import com.zhufu.opencraft.player_community.PlayerOutputStream
import com.zhufu.opencraft.util.WebOutputStream
import org.bukkit.configuration.ConfigurationSection
import java.io.File
import java.net.InetAddress
import java.util.*

abstract class WebInfo(createNew: Boolean,uuid: UUID? = null, nameToExtend: String? = null) : ServerPlayer(createNew,uuid,nameToExtend), ChatInfo {
    abstract val face: File
    override var doNotTranslate: Boolean = false
    override val displayName: String
        get() = "${name ?: "unknown"} Web"
    override val targetLang: String
        get() = userLanguage

    var lastExchange: HttpExchange? = null
    var exchanges = 0
    var streamsReturned = 0

    override val playerOutputStream: PlayerOutputStream
        get() {
            streamsReturned++

            val returned = streamsReturned
            while (returned != exchanges) {
                Thread.sleep(200)
                if (streamsReturned - exchanges
                    >= (ServerCaller["GetWebConfig"]!!(listOf())
                            as ConfigurationSection).getInt("chatPackLossThreshold")
                ) {
                    streamsReturned = 0
                    exchanges = 1
                    throw IllegalStateException("Request out of threshold. Change chatPackLossThreshold in config to a higher value.")
                }
            }
            return WebOutputStream(this)
        }
}