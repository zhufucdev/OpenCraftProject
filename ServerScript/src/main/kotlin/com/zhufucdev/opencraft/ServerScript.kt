package com.zhufucdev.opencraft

import com.zhufu.opencraft.Scripting
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

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
        print("Finished in ${end - start}ms.")
    }

    override fun onEnable() {
        initScripting()
    }

    override fun onDisable() {
        Scripting.cleanUp()
    }
}