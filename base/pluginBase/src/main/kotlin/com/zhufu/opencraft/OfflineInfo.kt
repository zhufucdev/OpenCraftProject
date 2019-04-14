package com.zhufu.opencraft

import com.zhufu.opencraft.player_intract.PlayerStatics
import org.bukkit.Bukkit
import org.bukkit.Location
import java.io.File
import java.nio.file.Paths
import java.util.*
import javax.security.auth.DestroyFailedException
import javax.security.auth.Destroyable

open class OfflineInfo(uuid: UUID, createNew: Boolean = false) : ServerPlayer(createNew,uuid), Destroyable {
    companion object {
        fun forEach(l: (OfflineInfo) -> Unit) = Paths.get("plugins","tag").toFile().also { if (!it.exists()) it.mkdirs() }.listFiles().forEach {
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

        fun findOfflinePlayer(uuid: UUID): OfflineInfo? =
                offlineList.firstOrNull { it.uuid == uuid }
                        ?: try {
                            OfflineInfo(uuid).also { offlineList.add(it) }
                        } catch (e: Exception) {
                            null
                        }

        val offlineList = ArrayList<OfflineInfo>()
    }

    val inventoriesFile
        get() = File("plugins${File.separatorChar}inventories${File.separatorChar}$uuid")
    override val tagFile: File
        get() = File(File("plugins${File.separatorChar}tag").also { if (!it.exists()) it.mkdirs() }, "$uuid.yml").also { register ->
            if (!register.exists()){
                val preregister = File(register.parentFile,"preregister${File.separatorChar}${offlinePlayer.name}.yml")
                if (preregister.exists()){
                    preregister.apply {
                        renameTo(register)
                        delete()
                    }
                }
            }
        }

    override fun isDestroyed(): Boolean = !memory.containsKey(uuid)
    override fun destroy() {
        if (isDestroyed) {
            throw DestroyFailedException()
        }
        memory.remove(uuid)
        PlayerStatics.remove(uuid!!)
        offlineList.removeAll { it.uuid == uuid }
    }

    override fun saveTag() {
        tag.set("checkpoints",null)
        checkpoints.forEach {
            tag.set("checkpoints.${it.id}", it.location)
        }
        super.saveTag()
    }

    class CheckpointInfo(val location: Location, var id: String) {
        override fun equals(other: Any?): Boolean {
            return other is CheckpointInfo
                    && other.location == this.location
                    && other.id == this.id
        }

        override fun hashCode(): Int {
            var result = location.hashCode()
            result = 31 * result + id.hashCode()
            return result
        }
    }

    var checkpoints = ArrayList<CheckpointInfo>()
        private set

    init {
        try {
            tag.set("name", offlinePlayer.name)
        } catch (e: Exception){

        }

        val checkpoints = tag.getConfigurationSection("checkpoints")
        checkpoints?.getKeys(false)?.forEach {
            try {
                this.checkpoints.add(CheckpointInfo(location = checkpoints.getSerializable(it, Location::class.java)!!, id = it))
            } catch (e: Exception) {
                e.printStackTrace()
            } as Unit
        }
    }

    fun delete() {
        tagFile.delete()
        inventoriesFile.delete()
        destroy()
    }
}