package com.zhufu.opencraft.data

import org.bukkit.Bukkit
import java.io.File
import java.nio.file.Paths
import java.util.*

class PreregisteredInfo : WebInfo {
    constructor(name: String) : super(true, nameToExtend = name, uuid = UUID.randomUUID())
    constructor(id: UUID) : super(false, id)
    companion object {
        fun exists(name: String) = Database.tag(name)?.let {
            val id = it.get("_id", UUID::class.java)
            val player = Bukkit.getOfflinePlayer(name)
            id != player.uniqueId
        } == true
    }
    init {
        name = name
    }

    override val playerDir: File
        get() = Paths.get("plugins", "playerDir", "preregister", name).toFile().also { if (!it.exists()) it.mkdirs() }
    override val face: File
        get() = Paths.get("plugins", "faces", "$name.png").toFile()

    val isRegistered: Boolean
        get() = offlineInfo != null
    val offlineInfo by lazy { OfflineInfo.findByName(name!!) }
}