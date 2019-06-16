package com.zhufu.opencraft.headers.npc_wrap

import net.citizensnpcs.api.trait.Trait
import java.util.function.Function

class SimpleTrait(private val onSpawn: Function<Any?, Any?>? = null) : Trait("simple_trait") {
    override fun onSpawn() {
        onSpawn?.apply(arrayOf(npc))
        npc.isProtected = false
    }
}