package com.zhufucdev.opencraft

import com.zhufu.opencraft.Game
import com.zhufu.opencraft.Scripting
import org.bukkit.plugin.java.JavaPlugin
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

class ServerScript : JavaPlugin() {
    @Suppress("MemberVisibilityCanBePrivate")
    fun initScripting() {
        logger.info("Loading scripts under plugins directory.")
        thread(name = "ServerScript Initialization") {
            val timeElapsed = measureTimeMillis {
                Scripting.cleanUp()
                Scripting.init(this).let { failures ->
                    if (failures.isEmpty()) return@let
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
            }
            logger.info("Finished in ${timeElapsed}ms")
        }
    }

    private var watchingThread: Thread? = null

    override fun onEnable() {
        initScripting()
    }

    override fun onDisable() {
        watchingThread?.interrupt()
        Scripting.cleanUp()
    }
}