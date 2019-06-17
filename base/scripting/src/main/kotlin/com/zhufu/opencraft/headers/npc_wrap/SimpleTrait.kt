package com.zhufu.opencraft.headers.npc_wrap

import com.zhufu.opencraft.script.ExecutionSchedule
import net.citizensnpcs.api.trait.Trait
import java.util.function.Consumer
import java.util.function.Function

class SimpleTrait(private val onSpawn: Function<Any?,Any?>? = null,private val schedule: ExecutionSchedule,private val sn: SimpleNPC) : Trait("simple_trait") {
    override fun onSpawn() {
        schedule.task {
            onSpawn?.apply(sn)
        }
        npc.isProtected = false
    }
}