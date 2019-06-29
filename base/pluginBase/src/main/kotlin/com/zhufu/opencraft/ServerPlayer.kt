package com.zhufu.opencraft

import com.google.common.cache.CacheBuilder
import com.zhufu.opencraft.player_community.Friend
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.player_community.PlayerStatics
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.permissions.ServerOperator
import java.io.File
import java.io.FileNotFoundException
import java.io.Reader
import java.nio.file.Paths
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

abstract class ServerPlayer(private val createNew: Boolean, var uuid: UUID? = null, private val nameToExtend: String? = null) : ServerOperator {
    companion object {
        val memory =
            CacheBuilder.newBuilder()
                .maximumSize(Bukkit.getMaxPlayers().toLong())
                .build<UUID, YamlConfiguration>()!!

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
    }

    override fun equals(other: Any?): Boolean = other is ServerPlayer && other.tagFile == this.tagFile

    abstract val tagFile: File
    abstract val playerDir: File
    private var privateTag: YamlConfiguration? = null
    var tag: YamlConfiguration
       get() = if (uuid != null) {
            memory[uuid!!, { initTag(createNew) }]
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
            if (uuid != null) memory.put(uuid!!, value)
            else privateTag = value
        }
    val friendship = ArrayList<Friend>().apply {
        val section = tag.getConfigurationSection("friendship")
        section?.getKeys(false)?.forEach {
            if (section.isSet("$it.uuid"))
                add(Friend(uuid = UUID.fromString(section.getString("$it.uuid")), itsFriend = this@ServerPlayer))
            else if (section.isSet("$it.name"))
                add(Friend(name = section.getString("$it.name"), itsFriend = this@ServerPlayer))
        }
    }

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

    open fun saveTag() {
        if (friendship.isNotEmpty())
            friendship.forEachIndexed { index, friend ->
                val key: String
                val value: String
                when {
                    friend.uuid != null -> {
                        key = "uuid"
                        value = friend.uuid.toString()
                    }
                    friend.name != null -> {
                        key = "name"
                        value = friend.name.toString()
                    }
                    else -> return@forEachIndexed
                }
                tag.set("friendship.$index.$key", value)
            }
        if (!messagePool.isEmpty) {
            tag.set("messages", messagePool.serialize())
        }
        tag.save(tagFile)
        if (PlayerStatics.contains(this))
            statics!!.save()
    }

    var password: String?
        get() = tag.getString("password", null)
        set(value) = tag.set("password", value)
    val inventoriesFile
        get() = Paths.get("plugins", "inventories", uuid.toString()).toFile()!!

    var currency: Long
        get() = tag.getLong("currency", 0)
        set(value) = tag.set("currency", value)
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
    val remainingDemoTime: Long
        get() = 90 * 60 * 1000L - gameTime
    var remainingSurveyChance: Int
        get() = tag.getInt("surveyChanceRemaining", 10)
        set(value) = tag.set("surveyChanceRemaining", value)

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
            if (uuid != null) Bukkit.getOfflinePlayer(uuid!!)
            else throw IllegalStateException("Cannot read offline player for an info with uuid null.")

    var name: String?
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

    override fun isOp(): Boolean = try {
        offlinePlayer.isOp
    } catch (e: Exception) {
        false
    }

    override fun setOp(value: Boolean) {
        offlinePlayer.isOp = value
    }

    override fun hashCode(): Int {
        var result = createNew.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + tagFile.hashCode()
        result = 31 * result + playerDir.hashCode()
        result = 31 * result + (privateTag?.hashCode() ?: 0)
        result = 31 * result + friendship.hashCode()
        result = 31 * result + messagePool.hashCode()
        return result
    }

    val statics get() = PlayerStatics.from(this)
    val messagePool = MessagePool.from(tag)

    var maxLoopExecution
        get() = tag.getLong("maxLoopExecution", 1000)
        set(value) = tag.set("maxLoopExecution", value)
    val scriptDir get() = File(playerDir, "script").also { if (!it.exists()) it.mkdirs() }
}