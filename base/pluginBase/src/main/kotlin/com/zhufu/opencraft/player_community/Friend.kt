package com.zhufu.opencraft.player_community

import com.zhufu.opencraft.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.Inventory
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths
import java.util.*
import kotlin.collections.HashMap

class Friend(
    val a: ServerPlayer,
    val b: ServerPlayer,
    var startAt: Long = -1,
    val id: UUID,
    extra: ConfigurationSection = YamlConfiguration()
) {
    var sharedInventory: Inventory? = null
    val sharedCheckpoints = arrayListOf<CheckpointInfo>()
    var transferred = extra.getLong("transfer", 0)
    var shareLocation = extra.getBoolean("shareLocation", false)

    val isFriend get() = startAt != -1L

    init {
        if (isFriend) {
            if (extra.isSet("sharedInventory")) {
                createSharedInventory()
                extra.getConfigurationSection("sharedInventory")!!.apply {
                    getKeys(false).forEach {
                        sharedInventory!!.setItem(it.toInt(), getItemStack(it))
                    }
                }
            }
            if (extra.isSet("sharedCheckpoints")) {
                val config = extra.getConfigurationSection("sharedCheckpoints")!!
                config.getKeys(false).forEach {
                    sharedCheckpoints.add(
                        CheckpointInfo(
                        location = config.getSerializable(it, Location::class.java) ?: return@forEach,
                            name = it
                    ))
                }
            }
        }
        cache[id] = this
    }

    fun save() {
        val section = YamlConfiguration()
        fun putLabel(who: ServerPlayer, type: Char) {
            if (who.uuid != null) {
                section.set("$type.uuid", who.uuid.toString())
            } else {
                section.set("$type.name", who.name)
            }
        }

        putLabel(a, 'a')
        putLabel(b, 'b')

        section.set("startAt", startAt)
        section.set("id", id.toString())

        val inventory = YamlConfiguration()
        sharedInventory?.forEachIndexed { index, itemStack ->
            inventory.set(index.toString(), itemStack)
        }
        section.set(
            "extra",
            YamlConfiguration().apply {
                if (sharedInventory?.any { it != null } == true)
                    set("sharedInventory", inventory)
                if (sharedCheckpoints.isNotEmpty()) {
                    set("sharedCheckpoints", null)
                    sharedCheckpoints.forEach {
                        set("sharedCheckpoints.${it.name}", it.location)
                    }
                }
                set("transfer", transferred)
                set("shareLocation", shareLocation)
            }
        )

        section.save(getDataFile(id))
    }

    fun createSharedInventory() {
        if (sharedInventory == null)
            sharedInventory = Bukkit.createInventory(null, 45)
    }

    var exists = true
        private set
    fun delete() {
        a.friendship.removeIf { it.id == id }
        b.friendship.removeIf { it.id == id }
        cache.remove(id)
        getDataFile(id).delete()
        exists = false
    }

    override fun equals(other: Any?): Boolean =
        other is Friend
                && other.id == id
                && other.startAt == startAt

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + a.hashCode()
        result = 31 * result + b.hashCode()
        result = 31 * result + startAt.hashCode()
        return result
    }

    companion object {
        private val cache = HashMap<UUID, Friend>()
        private val dataFolder = Paths.get("plugins", "friendData").toFile()

        fun getDataFile(id: UUID, createNew: Boolean = true) =
            File(dataFolder, "$id.yml")
                .also {
                    if (!it.exists()) {
                        if (!it.parentFile.exists())
                            it.parentFile.mkdirs()
                        if (createNew)
                            it.createNewFile()
                    }
                }

        @Throws(IllegalStateException::class)
        fun deserialize(uuid: UUID): Friend {
            val file = getDataFile(uuid, false)
            if (!file.exists())
                throw FileNotFoundException(file.path)
            val config = YamlConfiguration.loadConfiguration(file)
            return Friend(
                a = ServerPlayer.of(
                    uuid = config.getString("a.uuid")?.let { UUID.fromString(it) },
                    name = config.getString("a.name")
                ),
                b = ServerPlayer.of(
                    uuid = config.getString("b.uuid")?.let { UUID.fromString(it) },
                    name = config.getString("b.name")
                ),
                startAt = config.getLong("startAt"),
                id = uuid,
                extra = config.getConfigurationSection("extra") ?: YamlConfiguration()
            ).also {
                cache[uuid] = it
            }
        }

        fun of(uuid: UUID) = cache[uuid] ?: deserialize(uuid)

        fun from(a: ServerPlayer, b: ServerPlayer): Friend? {
            val filter: (Friend) -> Boolean = { (it.a == a && it.b == b) || (it.b == a && it.a == b) }
            val index = cache.values.firstOrNull(filter)
            if (index != null)
                return index
            dataFolder.listFiles()?.forEach {
                val uuid = UUID.fromString(it.nameWithoutExtension)
                try {
                    val f = deserialize(uuid)
                    if (filter(f)) {
                        cache[f.id] = f
                        return f
                    }
                } catch (ignored: Exception) {
                    getDataFile(uuid)
                }
            }
            return null
        }

        fun saveAll() {
            cache.values.forEach {
                it.save()
            }
        }
    }
}