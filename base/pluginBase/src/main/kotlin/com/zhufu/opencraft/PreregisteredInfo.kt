package com.zhufu.opencraft

import java.io.File
import java.net.InetAddress
import java.nio.file.Paths

class PreregisteredInfo(val id: String) : WebInfo(true) {
    companion object {
        fun exist(name: String) = File("plugins/tag/preregister/$name.yml").exists()
    }
    override val tagFile: File
        get() = File(File("plugins/tag/preregister").also { if (!it.exists()) it.mkdirs() },"$id.yml")
    override val playerDir: File
        get() = Paths.get("plugins","playerDir","preregister").toFile().also { if (!it.exists()) it.mkdirs() }
    override val face: File
        get() = File("plugins/faces/$id.png")
}