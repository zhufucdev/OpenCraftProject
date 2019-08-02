package com.zhufu.opencraft.headers.npc_wrap

import com.zhufu.opencraft.Language
import com.zhufu.opencraft.headers.player_wrap.SimpleEntity
import com.zhufu.opencraft.headers.player_wrap.SimpleLocation
import com.zhufu.opencraft.runSync
import com.zhufu.opencraft.script.ExecutionSchedule
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import java.util.function.Function

@Suppress("unused")
class SimpleNPC(val name: String, val spawnpoint: Location) {
    private lateinit var wrap: NPC
    private lateinit var simpleNavigator: SimpleNavigator

    val navigator get() = simpleNavigator
    fun despawn() = runSync { wrap.despawn() }
    val entity: Entity? get() = wrap.entity

    companion object {
        fun create(
            schedule: ExecutionSchedule,
            getter: Language.LangGetter,
            name: String,
            spawnpoint: Location,
            onSpawn: Function<Any?,Any?>? = null,
            navigator: Function<Any?,Any?>? = null,
            behavior: BehaviorCollection? = null,
            target: Any? = null,
            attack: Any? = null
        ): SimpleNPC {
            val npc = SimpleNPC(name, spawnpoint)
            var done = false
            runSync {
                npc.apply {
                    wrap = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name)
                    wrap.spawn(spawnpoint)

                    wrap.addTrait(SimpleTrait(onSpawn,schedule,npc))
                    simpleNavigator = SimpleNavigator(wrap.navigator)
                    schedule.task {
                        navigator?.apply(arrayOf(simpleNavigator))
                    }

                    if (attack != null && target != null){
                        schedule.task { throw IllegalArgumentException(getter["npc.error.parConflict","attack, target"]) }
                    }
                    if (behavior == null) {
                        if (attack == null) {
                            if (target != null)
                                when (target) {
                                    is SimpleLocation -> simpleNavigator.setTarget(target)
                                    is SimpleEntity -> simpleNavigator.setTarget(target)
                                    else -> schedule.task { throw IllegalArgumentException(getter["npc.error.wrongClass", "navigator"]) }
                                }
                        } else {
                            when (attack) {
                                is SimpleEntity -> simpleNavigator.attack(attack)
                                is Function<*, *> -> schedule.task {
                                    val r = (attack as Function<Any?, Any?>).apply(arrayOf(this))
                                    if (r is SimpleEntity) {
                                        simpleNavigator.attack(r)
                                    } else {
                                        throw IllegalStateException(getter["npc.error.returnWrongType", "attack/[lambda]"])
                                    }
                                }
                                else -> schedule.task { throw IllegalArgumentException(getter["npc.error.wrongClass", "attack"]) }
                            }
                        }
                    } else {
                        if (attack == null && target == null){
                            behavior.npc = this

                        } else schedule.task {
                            throw IllegalArgumentException(getter["npc.error.parConflict","behavior to (attack,target)"])
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

        fun recover(sn: SimpleNPC) = sn.wrap
    }
}