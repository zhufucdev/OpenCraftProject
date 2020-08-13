package com.zhufu.opencraft.task

import com.zhufu.opencraft.TaskManager
import com.zhufu.opencraft.chunkgenerator.VoidGenerator
import org.bukkit.World
import org.bukkit.generator.ChunkGenerator

/**
 * A separated-world task is where player plays RPG in another MC world.
 * It has its own lifespan differing from a [Task]:
 *  [onInit] (added to [TaskManager]) => [canStart] (returns true) => [onStart] => [complete] => [onStop]
 */
abstract class SeparatedWorldTask : Task() {
    open val worldGenerator: ChunkGenerator = VoidGenerator(60)

    /**
     * Called when this task is newly added to [TaskManager].
     * @param world A save where this task will be carried.
     */
    open fun onInit(world: World) {}
}