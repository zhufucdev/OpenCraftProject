package com.zhufu.opencraft.data

import java.io.File
import java.nio.file.Paths
import java.util.*

open class RegisteredInfo(uuid: UUID, createNew: Boolean = false) : WebInfo(createNew, uuid) {
    companion object {
        fun exists(uuid: UUID): Boolean = OfflineInfo.findByUUID(uuid) != null
    }
    override val playerDir: File
        get() = Paths.get("plugins", "playerDir", uuid.toString()).toFile().also {
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
        get() = Paths.get("plugins", "avatar", "$uuid.png").toFile()
}