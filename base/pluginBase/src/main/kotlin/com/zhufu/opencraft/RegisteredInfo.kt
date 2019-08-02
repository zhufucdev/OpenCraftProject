package com.zhufu.opencraft

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Paths
import java.util.*

class RegisteredInfo(uuid: UUID) : WebInfo(false, uuid) {
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

    override val tagFile: File
        get() = Paths.get("plugins", "tag", "$uuid.yml").toFile()
    override val playerDir: File
        get() = Paths.get("plugins", "playerDir", uuid.toString()).toFile().also {
            if (!it.exists()) it.mkdirs()
        }

    override val id: String
        get() = name ?: "unknown"
    override var doNotTranslate = false
    override val displayName get() = "$name Web"
    override val targetLang: String get() = this.userLanguage
    override val face: File
        get() = Paths.get("plugins", "faces", "$uuid.png").toFile()
}