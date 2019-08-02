package com.zhufu.opencraft.headers.npc_wrap

import com.zhufu.opencraft.headers.player_wrap.SimpleEntity
import com.zhufu.opencraft.headers.player_wrap.SimpleLocation
import net.citizensnpcs.api.ai.Navigator

@Suppress("unused")
class SimpleNavigator(private val wrap: Navigator){
    fun setTarget(sl: SimpleLocation){
        wrap.setTarget(SimpleLocation.recover(sl))
    }
    fun setTarget(entity: SimpleEntity){
        wrap.setTarget(SimpleEntity.recover(entity),false)
    }
    fun attack(entity: SimpleEntity) {
        wrap.setTarget(SimpleEntity.recover(entity),true)
    }
    companion object {
        fun recover(sn: SimpleNavigator) = sn.wrap
    }
}