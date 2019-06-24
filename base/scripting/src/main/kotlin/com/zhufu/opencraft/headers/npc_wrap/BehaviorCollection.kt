package com.zhufu.opencraft.headers.npc_wrap

import com.zhufu.opencraft.Language
import com.zhufu.opencraft.headers.npc_wrap.NPCUtils.Companion.validate
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter
import java.util.AbstractMap

class BehaviorCollection private constructor() {
    lateinit var npc: SimpleNPC
    val goals = arrayListOf<BehaviorGoalAdapter>()
    fun add(map: Any?){
        if (map is AbstractMap<*, *>){
            with(deserializeOne(map)){
                goals.add(this)
                SimpleNPC.recover(npc).defaultGoalController.addGoal(this,0)
            }
        } else {
            throw IllegalArgumentException("The first parameter must be an object.")
        }
    }
    companion object {
        fun deserializeOne(map: AbstractMap<*, *>): BehaviorGoalAdapter {
            TODO()
        }

        fun deserialize(map: Any?, getter: Language.LangGetter): BehaviorCollection {
            if (map is AbstractMap<*, *>) {
                val r = BehaviorCollection()
                map.forEach {
                    if (it.key.javaClass != Integer::class.java){
                        throw IllegalArgumentException(getter["npc.error.wrongClass","behavior"])
                    }
                    validate("behavior/${it.key}", AbstractMap::class.java, map, getter)
                    r.goals.add(deserializeOne(it.value as AbstractMap<*, *>))
                }
                return r
            } else {
                throw IllegalArgumentException(getter["npc.error.wrongClass", "behavior"])
            }
        }
    }
}