package com.zhufu.opencraft

import java.io.File
import java.nio.file.Paths

class PreregisteredInfo(override val id: String) : WebInfo(true, nameToExtend = id) {
    companion object {
        fun exists(name: String) = Paths.get("plugins", "tag", "preregister", "$name.yml").toFile().exists()
    }

    override val tagFile: File
        get() = File(
            Paths.get("plugins", "tag", "preregister").toFile().also { if (!it.exists()) it.mkdirs() },
            "$name.yml"
        )
    override val playerDir: File
        get() = Paths.get("plugins", "playerDir", "preregister", name).toFile().also { if (!it.exists()) it.mkdirs() }
    override val face: File
        get() = Paths.get("plugins", "faces", "$name.png").toFile()

    val isRegistered: Boolean
        get() = Paths.get("plugins", "tag", "$name.yml").toFile().exists()
    val offlineInfo by lazy { OfflineInfo.findByName(name!!) }
}