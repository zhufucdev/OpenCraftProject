package com.zhufu.opencraft.headers.npc_wrap

import com.zhufu.opencraft.Language
import com.zhufu.opencraft.headers.player_wrap.SimpleLocation
import com.zhufu.opencraft.runSync
import com.zhufu.opencraft.script.AbstractScript
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.ai.Navigator
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Location
import org.bukkit.entity.EntityType
import java.util.function.Function

class SimpleNPC(val name: String, val spawnpoint: Location) {
    private lateinit var wrap: NPC
    private lateinit var simpleNavigator: SimpleNavigator

    val navigator get() = simpleNavigator
    fun despawn() = wrap.despawn()

    companion object {
        fun deserialize(
            getter: Language.LangGetter,
            name: String,
            spawnpoint: Location,
            onSpawn: Function<Any?, Any?>? = null,
            navigator: Function<Any?,Any?>? = null,
            target: Any? = null,
            attack: Any? = null
        ): SimpleNPC {
            val npc = SimpleNPC(name, spawnpoint)
            var done = false
            runSync {
                npc.apply {
                    wrap = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name)
                    wrap.spawn(spawnpoint)

                    wrap.addTrait(SimpleTrait(onSpawn))
                    simpleNavigator = SimpleNavigator(wrap.navigator)
                    navigator?.apply(simpleNavigator)

                    if (attack != null && target != null){
                        throw IllegalArgumentException(getter["npc.error.parConflict","attack, target"])
                    }
                    if (attack == null) {
                        when (target) {
                            is SimpleLocation -> simpleNavigator.setTarget(target)
                            is SimpleEntity -> simpleNavigator.setTarget(target)
                            else -> throw IllegalArgumentException(getter["npc.error.wrongClass","navigator"])
                        }
                    } else {
                        when (attack){
                            is SimpleEntity -> simpleNavigator.setTarget(attack)
                            is Function<*, *> -> {
                                (attack as Function<Any?, Any?>).apply(this)
                            }
                            else -> throw IllegalArgumentException(getter["npc.error.wrongClass","attack"])
                        }
                    }
                    Unit
                }
                done = true
            }
            while (!done){
                Thread.sleep(200)
            }

            return npc
        }
    }
}