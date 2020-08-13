package com.zhufu.opencraft.task

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

class TaskStatus(
    val timeCreated: Long,
    var timeDone: Long = -1L,
    var active: Boolean = false,
    var initTask: BukkitTask? = null,
    var isInitialized: Boolean = false
) {
    var data: ConfigurationSection = YamlConfiguration()
        private set

    fun serialize(config: ConfigurationSection) {
        config.set("creation", timeCreated)
        config.set("duty", timeDone)
        if (data.getKeys(false).isNotEmpty())
            config.set("extra", data)
    }

    companion object {
        fun deserialize(config: ConfigurationSection): TaskStatus {
            val r = TaskStatus(timeCreated = config.getLong("creation"), timeDone = config.getLong("duty"))
            if (config.contains("extra"))
                r.data = config.getConfigurationSection("extra")!!
            return r
        }
    }
}