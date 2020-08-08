package com.zhufu.opencraft.npc

import net.citizensnpcs.api.trait.Trait
import net.citizensnpcs.api.trait.TraitName

@TraitName("movable")
class Movable: Trait("movable") {
    override fun onSpawn() {
        super.onSpawn()
        npc.navigator.setTarget(npc.entity, false)
    }
}