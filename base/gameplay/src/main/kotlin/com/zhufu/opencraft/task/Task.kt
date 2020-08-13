package com.zhufu.opencraft.task

import com.zhufu.opencraft.ServerPlayer
import com.zhufu.opencraft.TaskManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.Listener

/**
 * A task is where player plays RPG.
 * Any task has its lifespan:
 *  [canStart] (returns true) => [onStart] => [complete] => [onStop]
 * As soon as a server reload or sever stop is called, the [onSave] method is called.
 */
abstract class Task : Listener {
    var id: Int = ++ID
        private set
    abstract val owner: ServerPlayer
    val isCompleted: Boolean
        get() = TaskManager.status(this).timeDone != -1L
    val players: List<Player>
        get() = TaskManager.getTaskPlayers(this)
    val data: ConfigurationSection
        get() = TaskManager.status(this).data

    /**
     * Called when a new player joins this task.
     * @return True if this task can be started.
     */
    abstract fun canStart(): Boolean
    open fun onStart() {}
    open fun onStop() {}
    open fun onSave() {}

    /**
     * Mark this task as completed and delete its world.
     */
    protected open fun complete() {
        TaskManager.complete(this)
    }

    /**
     * Mark the give player not in this task.
     */
    protected open fun quit(player: Player) {
        TaskManager.quit(player)
    }

    override fun equals(other: Any?): Boolean = other is Task && other.id == this.id
    override fun hashCode(): Int = id.hashCode() * 31 + owner.hashCode() * 31

    companion object {
        private var ID = 0

        private val registered = arrayListOf<Class<out Task>>()
        fun register(clazz: Class<out Task>) {
            if (!registered.contains(clazz))
                registered.add(clazz)
        }

        fun deserialize(id: Int, cName: String, data: ConfigurationSection): Task? {
            val find =
                registered.firstOrNull { it.canonicalName?.let { n -> n == cName } == true || it.simpleName == cName }
                    ?: return null
            if (id > ID) ID = id
            return (find.getMethod("deserialize", ConfigurationSection::class.java).invoke(null, data) as Task).apply { this.id = id }
        }
    }
}