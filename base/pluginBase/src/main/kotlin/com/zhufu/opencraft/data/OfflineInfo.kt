package com.zhufu.opencraft.data

import java.io.File
import java.nio.file.Paths
import java.util.*

open class OfflineInfo(uuid: UUID, createNew: Boolean = false) : ServerPlayer(createNew, uuid) {
    companion object {
        fun forEach(l: (OfflineInfo) -> Unit) =
            Paths.get("plugins", "tag").toFile().also { if (!it.exists()) it.mkdirs() }.listFiles()?.forEach {
                if (!it.isHidden && !it.isDirectory) {
                    val loader = OfflineInfo(UUID.fromString(it.nameWithoutExtension))
                    l(loader)
                    loader.destroy()
                }
            }

        fun listPlayers(): List<OfflineInfo> {
            val r = ArrayList<OfflineInfo>()
            forEach { r.add(it) }
            return r
        }

        fun listPlayers(regex: (OfflineInfo) -> Boolean): List<OfflineInfo> {
            val r = ArrayList<OfflineInfo>()
            forEach { if (regex(it)) r.add(it) }
            return r
        }

        fun findByUUID(uuid: UUID): OfflineInfo? =
            cacheList.firstOrNull { it.uuid == uuid }
                ?: try {
                    OfflineInfo(uuid).also { cacheList.add(it) }
                } catch (e: Exception) {
                    null
                }
        fun findByName(name: String): OfflineInfo? {
            val r = cacheList.firstOrNull { it.name == name }
            if (r != null) {
                return r
            } else {
                Paths.get("plugins", "tag").toFile().listFiles()?.forEach {
                    if (!it.isHidden && !it.isDirectory) {
                        val i = OfflineInfo(UUID.fromString(it.nameWithoutExtension))
                        if (i.name == name)
                            return i
                    }
                }
            }
            return null
        }

        val cacheList = ArrayList<OfflineInfo>()
    }

    override val tagFile: File
        get() =
            File(
                Paths.get("plugins", "tag").toFile()
                    .also { if (!it.exists()) it.mkdirs() }, "$uuid.yml"
            ).also { register ->
                if (!register.exists()) {
                    val preregister =
                        Paths.get(
                            register.parent, "preregister", "${offlinePlayer.name}.yml"
                        ).toFile()
                    if (preregister.exists()) {
                        preregister.apply {
                            renameTo(register)
                            delete()
                        }
                    }
                }
            }
    override val playerDir: File
        get() = Paths.get("plugins", "playerDir", uuid.toString()).toFile()
            .also {
                val preregister = Paths.get(it.parentFile.path, "preregister", uuid.toString()).toFile()
                if (preregister.exists()) {
                    if (it.exists()) it.deleteRecursively()
                    preregister.renameTo(it)
                } else {
                    if (!it.exists()) it.mkdirs()
                }
            }

    override fun destroy() {
        cacheList.removeAll { it.uuid == uuid }
        super.destroy()
    }

    override fun delete() {
        destroy()
    }
}