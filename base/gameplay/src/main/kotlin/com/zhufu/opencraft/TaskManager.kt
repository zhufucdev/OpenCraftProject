package com.zhufu.opencraft

import com.zhufu.opencraft.task.Task
import com.zhufu.opencraft.task.TaskStatus
import com.zhufu.opencraft.task.SeparatedWorldTask
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.io.File
import java.nio.file.Paths

object TaskManager : Listener {
    private val tasks = hashMapOf<Task, TaskStatus>()
    private val worlds = hashMapOf<SeparatedWorldTask, World>()
    private fun init(task: Task, status: TaskStatus, done: (() -> Unit)?) {
        if (status.isInitialized) {
            done?.invoke()
            return
        }
        Bukkit.getPluginManager().registerEvents(task, plugin)
        if (task is SeparatedWorldTask) {
            // Create a new world with specific name and generator.
            val world = Bukkit.createWorld(
                WorldCreator.name("task-${task.id}")
                    .generator(task.worldGenerator)
            )!!
            worlds[task] = world
            // Start a new thread to initialize the task.
            status.initTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                task.onInit(world)
                done?.invoke()
                status.initTask = null
            })
        } else {
            done?.invoke()
        }
        status.isInitialized = true
    }

    @Synchronized
    fun add(task: Task, initComplete: (() -> Unit)? = null) {
        checkInitialized()
        if (tasks.contains(task)) return
        val status = TaskStatus(
            timeCreated = System.currentTimeMillis(),
            active = false
        )
        tasks[task] = status
        init(task, status, initComplete)
    }

    private fun start(task: Task, status: TaskStatus, done: (() -> Unit)? = null) {
        status.active = true
        fun start() {
            Bukkit.getScheduler().runTask(plugin) { _ ->
                try {
                    task.onStart()
                    done?.invoke()
                } catch (e: Exception) {
                    task.players.forEach {
                        it.error(it.getter()["rpg.ui.task.error.start", task.id, e::class.simpleName, e.message])
                    }
                    e.printStackTrace()
                }
            }
        }
        init(task, status) {
            // If the task has not completed initialization
            if (status.initTask != null) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    // Wait for complete
                    while (status.initTask != null) {
                        Thread.sleep(200)
                    }
                    start()
                })
            } else {
                start()
            }
        }
    }

    private val playerTaskMap = hashMapOf<Player, Task>()
    fun getTaskPlayers(task: Task): List<Player> {
        val r = arrayListOf<Player>()
        playerTaskMap.forEach { (t, u) ->
            if (u == task)
                r.add(t)
        }
        return r
    }

    private val previousStatus = hashMapOf<Player, Info.GameStatus>()
    fun join(player: Player, to: Task, done: (() -> Unit)? = null) {
        checkInitialized()
        checkExistence(to)
        player.info()?.status?.let { previousStatus[player] = it }
        playerTaskMap[player] = to
        if (to.canStart()) {
            start(to, status(to), done)
        }
    }

    fun quit(player: Player) {
        val task = playerTaskMap.remove(player)
        if (task?.players?.isEmpty() == true)
            complete(task)
    }

    private var initialized = false
    private lateinit var plugin: Plugin
    fun init(plugin: Plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        initialized = true
        this.plugin = plugin
        reload()
    }

    fun reload() {
        tasks.clear()
        root.listFiles()?.forEach {
            val id = it.nameWithoutExtension.toIntOrNull()
            if (id == null) {
                Bukkit.getLogger().warning("[TaskManager] Name of ${it.path} is illegal.")
                return@forEach
            }
            try {
                val yaml = YamlConfiguration.loadConfiguration(it)
                val status = TaskStatus.deserialize(yaml)
                val type = yaml.getString("type")!!
                val extra = if (yaml.contains("extra")) yaml.getConfigurationSection("extra")!! else YamlConfiguration()
                val task = Task.deserialize(id, type, extra) ?: error("No such type of task: $type.")
                tasks[task] = status
            } catch (e: Exception) {
                Bukkit.getLogger().warning("[TaskManager] Could not deserialize ${it.path}:")
                e.printStackTrace()
            }
        }
    }

    private val root = Paths.get("plugins", "tasks").toFile()
    fun save() {
        if (!root.exists()) root.mkdirs()
        val idInclude = arrayListOf<Int>()
        tasks.forEach { (t, u) ->
            val file = File(root, "${t.id}.yml")
            idInclude.add(t.id)
            val yaml = YamlConfiguration()
            yaml.set("type", t::class.let { it.qualifiedName ?: it.simpleName })
            t.onSave()
            u.serialize(yaml)
            yaml.save(file)
        }
        root.listFiles()!!.forEach {
            val id = it.nameWithoutExtension.toIntOrNull()
            if (id == null || !idInclude.contains(id)) it.delete()
        }
    }

    operator fun get(owner: ServerPlayer): List<Task> {
        checkInitialized()
        return tasks.keys.filter { it.owner == owner }
    }

    fun status(task: Task): TaskStatus {
        checkExistence(task)
        return tasks[task]!!
    }

    fun contains(task: Task) = tasks.contains(task)

    fun complete(task: Task) {
        checkInitialized()
        checkExistence(task)
        val status = tasks[task]!!
        // Mark inactive
        status.apply {
            timeDone = System.currentTimeMillis()
            active = false
        }
        task.players.forEach {
            if (previousStatus.containsKey(it))
                it.info()?.let { info ->
                    info.status = previousStatus[it]!!
                }
            playerTaskMap.remove(it)
        }
        Bukkit.getScheduler().runTask(plugin) { _ ->
            task.onStop()
            if (task is SeparatedWorldTask) {
                // Delete world
                val world = worlds[task]!!
                Bukkit.unloadWorld(world, false)
                File(Bukkit.getWorldContainer(), world.name).deleteRecursively()
                worlds.remove(task)
            }
        }
    }

    private fun checkInitialized() {
        if (!initialized) error("TaskManger has not been initialized yet.")
    }

    private fun checkExistence(task: Task) {
        if (!contains(task)) error("Task ${task.id} has not been added to TaskManager.")
    }
}