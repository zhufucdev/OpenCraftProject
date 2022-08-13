package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.TradeInfo
import org.bukkit.Location
import javax.security.auth.Destroyable

typealias Producer = (TradeInfo, Location?) -> NPCExistence

interface NPCExistence : Destroyable {
    companion object {
        internal lateinit var impl: Producer
        fun setProducer(impl: Producer) {
            this.impl = impl
        }
    }
}