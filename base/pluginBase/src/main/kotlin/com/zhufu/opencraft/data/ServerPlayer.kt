package com.zhufu.opencraft.data

import com.google.common.cache.CacheBuilder
import com.zhufu.opencraft.Base
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.ServerStatics
import com.zhufu.opencraft.player_community.Friendships
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.player_community.PlayerStatics
import com.zhufu.opencraft.updateItemMeta
import org.bson.Document
import org.bson.types.Binary
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.permissions.ServerOperator
import java.io.File
import java.security.MessageDigest
import java.util.*
import javax.security.auth.DestroyFailedException
import javax.security.auth.Destroyable

abstract class ServerPlayer(
    createNew: Boolean,
    val uuid: UUID,
    private val nameToExtend: String? = null
) : ServerOperator, Destroyable {
    companion object {
        fun forEachSaved(l: (ServerPlayer) -> Unit) {
            Database.tag.find().forEach {
                val id = it.get("_id", UUID::class.java)
                if (Bukkit.getPlayer(id) == null) {
                    l(PreregisteredInfo(id))
                } else {
                    l(OfflineInfo(id, false))
                }
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
            else -> throw IllegalArgumentException()
        }

        private val cache = hashMapOf<UUID, Document>()
        private fun tag(uuid: UUID, createNew: Boolean) =
            cache[uuid]
                ?: Database.tag(uuid, createNew)!!.also { cache[uuid] = it }
    }

    val doc: Document = tag(uuid, createNew)
    internal fun update() {
        Database.tag(uuid, doc)
    }

    abstract val playerDir: File
    val friendships get() = Friendships.of(this)
    val checkpoints get() = Checkpoints.of(this)
    val sharedCheckpoints: List<Pair<ServerPlayer, Checkpoint>>
        get() = friendships.mapNotNull { friend ->
            if (friend.isFriend) friend.sharedCheckpoints.forEach { point ->
                if (!checkpoints.contains(point))
                    return@mapNotNull friend.friend to point
            }
            null
        }
    fun removeCheckpoint(name: String): Boolean {
        val index = checkpoints.firstOrNull { it.name == name }
        return if (index != null) {
            checkpoints.remove(index)
            friendships.forEach {
                it.removeSharedCheckpoint(index)
            }
            true
        } else {
            false
        }
    }

    val hasPassword: Boolean
        get() = doc.containsKey("password")
    fun setPassword(pwd: String?) {
        if (pwd == null) {
            doc.remove("password")
        } else {
            val md = MessageDigest.getInstance("SHA-256")
            val encoded = pwd.encodeToByteArray()
            val bytes = md.digest(encoded)
            doc["password"] = bytes
        }
        update()
    }
    fun matchPassword(pwd: String): Boolean {
        if (!doc.containsKey("password"))
            throw NullPointerException("Password not set.")

        val md = MessageDigest.getInstance("SHA-256")
        val encoded = pwd.encodeToByteArray()
        val encrypt = md.digest(encoded)
        return encrypt.contentEquals(doc.get("password", Binary::class.java).data)
    }

    var currency: Long
        get() = doc.getLong("currency") ?: 0
        set(value) {
            statics?.setCurrency(value)
            doc["currency"] = value
            update()
        }
    var territoryID: Int
        get() = doc.getInteger("territoryID", -1)
        set(value) {
            doc["territoryID"] = value
            update()
        }
    val isOnline: Boolean
        get() = Bukkit.getOnlinePlayers().any { it.uniqueId == uuid }
    val onlinePlayerInfo: Info?
        get() = if (isOnline) Info.findByPlayer(uuid) else null

    var isSurveyPassed: Boolean
        get() = doc.getBoolean("isSurveyPassed", false)
        set(value) {
            doc["isSurveyPassed"] = value
            update()
        }
    var gameTime: Long
        get() = doc.getLong("gameTime") ?: 0
        set(value) {
            doc["gameTime"] = value
            update()
        }
    var damageDone: Double
        get() = doc.getDouble("damage") ?: 0.0
        set(value) {
            doc["damage"] = value
            update()
        }
    val remainingDemoTime: Long
        get() = 90 * 60 * 1000L - gameTime
    var remainingSurveyChance: Int
        get() = doc.getInteger("surveyChanceRemaining", 10)
        set(value) {
            doc["surveyChanceRemaining"] = value
            update()
        }
    val preference: PlayerPreference = PlayerPreference(doc)

    var userLanguage: String
        get() = doc.getString("lang") ?: Language.defaultLangCode
        set(value) {
            doc["lang"] = value
            update()
        }
    val locale: Locale get() = when (userLanguage) {
        Language.LANG_ZH -> Locale.SIMPLIFIED_CHINESE
        Language.LANG_EN -> Locale.ENGLISH
        else -> Base.locale
    }
    val isUserLanguageSelected: Boolean
        get() = doc.contains("lang")
    var nickname: String?
        get() = doc.getString("nickname")
        set(value) {
            if (value != null) {
                doc["nickname"] = value
            } else {
                doc.remove("nickname")
            }
            update()
        }

    val offlinePlayer: OfflinePlayer
        get() = Bukkit.getOfflinePlayer(uuid)

    open var name: String?
        get() = nameToExtend
            ?: doc.getString("name")
            ?: try {
                offlinePlayer.name
            } catch (e: IllegalStateException) {
                null
            }
        set(value) {
            if (value != null) {
                doc["name"] = value
            } else {
                doc.remove("name")
            }
            update()
        }
    var skin: String?
        get() = doc.getString("skin")
        set(value) {
            if (value != null) {
                doc["skin"] = value
            } else {
                doc.remove("skin")
            }
            update()
        }

    var builderLevel: Int
        get() = doc.getInteger("builder", 0)
        set(value) {
            if (value > 0) {
                doc["builder"] = value
            } else {
                doc.remove("builder")
            }
            update()
        }
    val isBuilder get() = builderLevel > 0
    var isSurvivor
        get() = doc.getBoolean("isSurvivor", false)
        set(value) {
            doc["isSurvivor"] = value
            update()
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

    init {
        if (territoryID == -1) {
            territoryID = ServerStatics.playerCount
        }
    }

    open fun delete() {
        Database.drop(uuid)
    }

    private var isDestroyed = false
    override fun isDestroyed(): Boolean = isDestroyed
    override fun destroy() {
        if (isDestroyed) {
            throw DestroyFailedException()
        }

        friendships.destroy()
        MessagePool.remove(this)
        PlayerStatics.remove(uuid)
        isDestroyed = true
    }

    override fun equals(other: Any?): Boolean {
        return other is ServerPlayer && other.uuid == this.uuid
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + playerDir.hashCode()
        return result
    }

    val statics get() = PlayerStatics.from(this)
    val messagePool get() = MessagePool.of(this)
    val scriptDir get() = File(playerDir, "script").also { if (!it.exists()) it.mkdirs() }
}