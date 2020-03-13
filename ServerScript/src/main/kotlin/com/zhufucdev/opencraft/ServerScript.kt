package com.zhufucdev.opencraft

import com.zhufu.opencraft.Game
import com.zhufu.opencraft.Scripting
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds
import kotlin.concurrent.thread

class ServerScript : JavaPlugin() {
    @Suppress("MemberVisibilityCanBePrivate")
    fun initScripting() {
        logger.info("Loading scripts under plugins directory.")
        val start = System.currentTimeMillis()
        Scripting.cleanUp()
        Scripting.init(this).let { failures ->
            if (failures.isEmpty()) return
            logger.warning {
                buildString {
                    append("Failed to load following server scripts: ")
                    failures.forEach {
                        append(it.nameWithoutExtension)
                        append(", ")
                    }
                }.removeSuffix(", ")
            }
        }
        val end = System.currentTimeMillis()
        if (Game.env.getBoolean("debug")) {
            print("Finished in ${end - start}ms.")
        }
        if (Game.env.getBoolean("ssHotReload")) {
            startWatchService()
        } else if (isWatching) {
            isWatching = false
            watchingThread!!.interrupt()
        }
    }

    private var isWatching = false
    private var watchingThread: Thread? = null
    private fun startWatchService() {
        if (isWatching) return
        isWatching = true

        watchingThread = thread {
            val watcher = FileSystems.getDefault().newWatchService()
            val path = Scripting.modulesDir.toPath()
            path.register(
                watcher,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            )
            while (isWatching) {
                val watchKey = watcher.take()
                initScripting()
                watchKey.reset()
            }
        }
    }

    override fun onEnable() {
        initScripting()
    }

    override fun onDisable() {
        isWatching = false
        watchingThread?.interrupt()
        Scripting.cleanUp()
    }
}