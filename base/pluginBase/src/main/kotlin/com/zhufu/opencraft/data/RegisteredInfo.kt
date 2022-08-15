package com.zhufu.opencraft.data

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Paths
import java.util.*

open class RegisteredInfo(uuid: UUID, createNew: Boolean = false) : WebInfo(createNew, uuid) {
    companion object {
        fun exists(uuid: UUID): Boolean {
            val file = Paths.get("plugins", "tag", "$uuid.yml").toFile()
            if (!file.exists())
                return false
            val data = try {
                YamlConfiguration.loadConfiguration(file)
            } catch (e: Exception) {
                return false
            }
            return data.isSet("password")
        }
    }
    override val playerDir: File
        get() = Paths.get("plugins", "playerDir", uuid.toString()).toFile().also {
            if (!it.exists()) it.mkdirs()
            val preregister = Paths.get(it.parentFile.path, "preregister", uuid.toString()).toFile()
            if (preregister.exists()) {
                if (it.exists()) it.deleteRecursively()
                preregister.renameTo(it)
            } else {
                if (!it.exists()) it.mkdirs()
            }
        }

    override var doNotTranslate = false
    override val displayName get() = "$name Web"
    override val targetLang: String get() = this.userLanguage
    override val face: File
        get() = Paths.get("plugins", "faces", "$uuid.png").toFile()
}