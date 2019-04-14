package com.zhufu.opencraft.manager

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.*

object RPGManager {
    private val root = File("plugins/rpg")
    fun init(){
        if (!root.exists() || !root.isDirectory){
            root.mkdirs()
        }
    }

    enum class RPGKit{
        WARRIOR,PRIEST,CODER,ARCHER,NONE
    }

    open class OfflineRPGPlayer{
        val uuid: UUID
        private val config: YamlConfiguration
        constructor(uuid: UUID){
            this.uuid = uuid
            config = YamlConfiguration.loadConfiguration(File(root,"$uuid.yml").also { if (!it.exists()) it.createNewFile() })
        }
        constructor(player: Player): this(player.uniqueId)

        var kit: RPGKit
            get() = RPGKit.valueOf(config.getString("kit","none")!!.toUpperCase())
            set(value) = config.set("kit",value.name.toLowerCase())
        val isKitSelected: Boolean
            get() = kit == RPGKit.NONE
        val level: Int
            get() = config.getInt("level",0)
    }

    class RPGPlayer(player: Player): OfflineRPGPlayer(player){

    }
}