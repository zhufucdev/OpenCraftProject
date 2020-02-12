package com.zhufu.opencraft

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player

object WorldManager {
    private lateinit var config: FileConfiguration
    fun init(configuration: FileConfiguration) {
        config = configuration
    }

    class mWorld(val world: World, permission: WorldPermissions, description: String = "") {
        private var mPermission = permission
        var permission: WorldPermissions
            get() = mPermission
            set(value) {
                mPermission = value
                config["${world.name}.permission"] = mPermission.name
            }

        private var mDescription = description
        var description: String
            get() = mDescription
            set(value) {
                mDescription = value
                config["${world.name}.description"] = mDescription
            }
    }

    enum class WorldPermissions {
        PUBLIC, PRIVATE, PROTECTED;

        fun canUse(sender: CommandSender) = (this == PUBLIC)
                || (this == PRIVATE && (sender !is Player || sender.isOp))

        fun canSee(sender: CommandSender) = (this == PUBLIC)
                || ((this == PRIVATE || this == PROTECTED) && (sender !is Player || sender.isOp))

        companion object {
            fun valueOf(value: String?, def: WorldPermissions): WorldPermissions = when {
                value.isNullOrEmpty() -> def
                value == "private" -> PRIVATE
                value == "protected" -> PROTECTED
                value == "public" -> PUBLIC
                else -> def
            }
        }
    }

    fun getAvailableWorlds(): List<mWorld> {
        val r = ArrayList<mWorld>()
        Bukkit.getWorlds().forEach {
            if (it.name.startsWith("game_")) {
                return@forEach
            }
            val per = config.getString("${it.name}.permission")
            val des = config.getString("${it.name}.description")
            r.add(mWorld(it, WorldPermissions.valueOf(per, WorldPermissions.PUBLIC), des ?: ""))
        }
        return r.toList()
    }
}