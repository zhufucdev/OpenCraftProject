package com.zhufu.opencraft

import com.zhufu.opencraft.player_community.PlayerStatics
import org.bukkit.Location
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.security.auth.DestroyFailedException
import javax.security.auth.Destroyable

open class OfflineInfo(uuid: UUID, createNew: Boolean = false) : ServerPlayer(createNew, uuid) {
    companion object {
        fun forEach(l: (OfflineInfo) -> Unit) =
            Paths.get("plugins", "tag").toFile().also { if (!it.exists()) it.mkdirs() }.listFiles()?.forEach {
                if (!it.isHidden && !it.isDirectory)
                    l(OfflineInfo(UUID.fromString(it.nameWithoutExtension)))
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
            offlineList.firstOrNull { it.uuid == uuid }
                ?: try {
                    OfflineInfo(uuid).also { offlineList.add(it) }
                } catch (e: Exception) {
                    null
                }

        val offlineList = ArrayList<OfflineInfo>()
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
        if (isDestroyed) {
            throw DestroyFailedException()
        }
        PlayerStatics.remove(uuid!!)
        offlineList.removeAll { it.uuid == uuid }
        super.destroy()
    }

    override fun saveTag() {
        tag.set("checkpoints", null)
        checkpoints.forEach {
            tag.set("checkpoints.${it.name}", it.location)
        }
        super.saveTag()
    }

    class CheckpointInfo(val location: Location, override var name: String) : Nameable {
        override fun equals(other: Any?): Boolean {
            return other is CheckpointInfo
                    && other.location == this.location
                    && other.name == this.name
        }

        override fun hashCode(): Int {
            var result = location.hashCode()
            result = 31 * result + name.hashCode()
            return result
        }
    }

    var checkpoints = ArrayList<CheckpointInfo>()
        private set

    init {
        val checkpoints = tag.getConfigurationSection("checkpoints")
        checkpoints?.getKeys(false)?.forEach {
            try {
                this.checkpoints.add(
                    CheckpointInfo(
                        location = checkpoints.getSerializable(it, Location::class.java)!!,
                        name = it
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun delete() {
        destroy()
    }
}