package com.zhufu.opencraft.player_intract

import com.zhufu.opencraft.OfflineInfo
import com.zhufu.opencraft.PreregisteredInfo
import com.zhufu.opencraft.ServerPlayer
import java.util.*

class Friend(val name: String? = null,val uuid: UUID? = null,val itsFriend: ServerPlayer){
    val info: ServerPlayer get() = if (uuid != null) OfflineInfo(uuid,false) else if (name != null) PreregisteredInfo(name) else throw IllegalStateException()
}