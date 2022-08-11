package com.zhufu.opencraft.data

import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.ServerStatics
import com.zhufu.opencraft.player_community.Friendship
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.player_community.PlayerStatics
import com.zhufu.opencraft.updateItemMeta
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.permissions.ServerOperator
import java.io.File
import java.nio.file.Paths
import java.util.*
import javax.security.auth.DestroyFailedException
import javax.security.auth.Destroyable
import kotlin.collections.HashMap

abstract class ServerPlayer(
    private val createNew: Boolean,
    val uuid: UUID? = null,
    private val nameToExtend: String? = null
) : ServerOperator, Destroyable {
    companion object {
        val memory = HashMap<UUID, YamlConfiguration>()

        val size: Int
            get() {
                var r = 0
                Paths.get("plugins", "tag").toFile().listFiles()?.forEach {
                    if (!it.isHidden && !it.isDirectory) {
                        r++
                    }
                }
                r += Paths.get("plugins", "tag", "preregister").toFile().listFiles()?.size ?: 0
                return r
            }

        fun forEachSaved(l: (ServerPlayer) -> Unit) {
            Paths.get("plugins", "tag").toFile().listFiles()?.forEach {
                if (!it.isHidden && !it.isDirectory)
                    l(OfflineInfo(UUID.fromString(it.nameWithoutExtension)))
            }
            Paths.get("plugins", "tag", "preregister").toFile().listFiles()?.forEach {
                if (!it.isHidden)
                    l(PreregisteredInfo(it.nameWithoutExtension))
            }
        }

        fun of(name: String? = null, uuid: UUID? = null) = when {
            uuid != null -> OfflineInfo(uuid, false)
            name != null -> {
                if (PreregisteredInfo.exists(name)) {
                    PreregisteredInfo(name)
                } else {
                    val offlineInfo = (OfflineInfo.findByName(name) ?: throw IllegalStateException())
                    if (offlineInfo.isOnline)
                        offlineInfo.onlinePlayerInfo!!
                    else
                        offlineInfo
                }
            }
            else -> throw IllegalStateException()
        }
    }

    override fun equals(other: Any?): Boolean = other is ServerPlayer && other.tagFile == this.tagFile

    abstract val tagFile: File
    abstract val playerDir: File
    private var privateTag: YamlConfiguration? = null
    var tag: YamlConfiguration
        get() = if (uuid != null) {
            memory[uuid].let {
                if (it == null) {
                    val t = initTag(createNew)
                    memory[uuid] = t
                    t
                } else {
                    it
                }
            }
        } else {
            privateTag.let {
                if (it == null) {
                    val r = initTag(createNew)
                    privateTag = r
                    r
                } else {
                    privateTag!!
                }
            }
        }
        set(value) {
            if (uuid != null) memory[uuid] = value
            else privateTag = value
        }
    val friendship get() = Friendship.from(this)

    private fun initTag(createNew: Boolean) = try {
        if (tagFile.exists())
            YamlConfiguration.loadConfiguration(tagFile)
        else {
            ServerStatics.playerNumber++
            if (createNew) {
                tagFile.createNewFile()
            }
            YamlConfiguration()
        }
    } catch (e: Throwable) {
        println("Could not load tag for player $uuid")
        throw e
    }

    open fun save() {
        if (friendship.isNotEmpty())
            friendship.save()
        if (!messagePool.isEmpty) {
            tag.set("messages", messagePool.serialize())
        }
        tag.set("checkpoints", null)
        checkpoints.forEach {
            tag.set("checkpoints.${it.name}", it.location)
        }
        try {
            tag.save(tagFile)
        } catch (e: Exception) {
            Bukkit.getLogger().warning("Failed to save $name's tag file:")
            e.printStackTrace()
        }
        if (PlayerStatics.contains(this))
            statics!!.save()
    }

    var checkpoints = arrayListOf<CheckpointInfo>().apply {
        val checkpoints = tag.getConfigurationSection("checkpoints")
        checkpoints?.getKeys(false)?.forEach {
            try {
                add(
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
        private set
    val sharedCheckpoints: List<Pair<ServerPlayer, CheckpointInfo>>
        get() {
            val r = arrayListOf<Pair<ServerPlayer, CheckpointInfo>>()
            friendship.forEach { friend ->
                if (friend.isFriend) friend.sharedCheckpoints.forEach { point ->
                    if (!checkpoints.contains(point))
                        r.add(friend.friend to point)
                }
            }
            return r
        }

    fun removeCheckpoint(name: String): Boolean {
        val index = checkpoints.firstOrNull { it.name == name }
        return if (index != null) {
            checkpoints.remove(index)
            friendship.forEach {
                it.sharedCheckpoints.remove(index)
            }
            true
        } else {
            false
        }
    }

    var password: String?
        get() = tag.getString("password", null)
        set(value) = tag.set("password", value)
    val inventoriesFile: File
        get() = Paths.get("plugins", "inventories", uuid.toString()).toFile()

    var currency: Long
        get() = tag.getLong("currency", 0)
        set(value) {
            statics?.setCurrency(value)
            tag.set("currency", value)
        }
    var territoryID: Int
        get() = tag.getInt("territoryID", -1).let {
            if (it == -1) {
                val r = ServerStatics.playerNumber - 1
                territoryID = r
                r
            } else
                it
        }
        set(value) {
            tag.set("territoryID", value)
        }
    val isOnline: Boolean
        get() = Bukkit.getOnlinePlayers().any { it.uniqueId == uuid }
    val onlinePlayerInfo: Info?
        get() = if (isOnline) Info.findByPlayer(uuid!!) else null

    var isSurveyPassed: Boolean
        get() = tag.getBoolean("isSurveyPassed", false)
        set(value) = tag.set("isSurveyPassed", value)
    var gameTime: Long
        get() = tag.getLong("gameTime", 0)
        set(value) = tag.set("gameTime", value)
    var damageDone: Double
        get() = tag.getDouble("damage", 0.0)
        set(value) = tag.set("damage", value)
    val remainingDemoTime: Long
        get() = 90 * 60 * 1000L - gameTime
    var remainingSurveyChance: Int
        get() = tag.getInt("surveyChanceRemaining", 10)
        set(value) = tag.set("surveyChanceRemaining", value)
    val preference: PlayerPreference = PlayerPreference(tag)

    var userLanguage: String
        get() = tag.getString("lang", Language.LANG_ZH)!!
        set(value) = tag.set("lang", value)
    val isUserLanguageSelected: Boolean
        get() = tag.contains("lang")
    var nickname: String?
        get() = tag.getString("nickname", null)
        set(value) = tag.set("nickname", value)

    val offlinePlayer: OfflinePlayer
        get() =
            if (uuid != null) Bukkit.getOfflinePlayer(uuid)
            else throw IllegalStateException("Cannot read offline player for an info with uuid null.")

    open var name: String?
        get() = nameToExtend
            ?: tag.getString("name")
            ?: try {
                offlinePlayer.name
            } catch (e: IllegalStateException) {
                null
            }
        set(value) = tag.set("name", value)
    var skin: String?
        get() = tag.getString("skin", null)
        set(value) = tag.set("skin", value)

    var builderLevel: Int
        get() = tag.getInt("builder", 0)
        set(value) {
            tag.set(
                "builder",
                if (value > 0) value else null
            )
        }
    val isBuilder get() = builderLevel > 0
    var isSurvivor
        get() = tag.getBoolean("isSurvivor", false)
        set(value) {
            tag.set("isSurvivor", value)
        }
    private val skull by lazy {
        ItemStack(Material.PLAYER_HEAD).updateItemMeta<SkullMeta> {
            owningPlayer = offlinePlayer
        }
    }
    val skullItem
        get() = skull.clone()

    override fun isOp(): Boolean = try {
        offlinePlayer.isOp
    } catch (e: Exception) {
        false
    }

    override fun setOp(value: Boolean) {
        offlinePlayer.isOp = value
    }

    open fun delete() {
        tagFile.delete()
        inventoriesFile.delete()
    }

    private var isDestroyed = false
    override fun isDestroyed(): Boolean = isDestroyed
    override fun destroy() {
        if (isDestroyed) {
            throw DestroyFailedException()
        }

        save()
        friendship.destroy()
        MessagePool.remove(this)
        if (uuid != null) {
            memory.remove(uuid)
            PlayerStatics.remove(uuid)
        }
        isDestroyed = true
    }

    override fun hashCode(): Int {
        var result = uuid?.hashCode() ?: 0
        result = 31 * result + tagFile.hashCode()
        result = 31 * result + playerDir.hashCode()
        return result
    }

    val statics get() = PlayerStatics.from(this)
    val messagePool get() = MessagePool.of(this)

    var maxLoopExecution
        get() = tag.getLong("maxLoopExecution", 1000)
        set(value) = tag.set("maxLoopExecution", value)
    val scriptDir get() = File(playerDir, "script").also { if (!it.exists()) it.mkdirs() }
}