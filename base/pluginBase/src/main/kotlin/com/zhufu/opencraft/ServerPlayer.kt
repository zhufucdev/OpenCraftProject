package com.zhufu.opencraft

import com.zhufu.opencraft.player_intract.Friend
import com.zhufu.opencraft.player_intract.MessagePool
import com.zhufu.opencraft.player_intract.PlayerStatics
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.FileNotFoundException
import java.io.Reader
import java.io.StringReader
import java.nio.file.Paths
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

abstract class ServerPlayer(private val createNew: Boolean, var uuid: UUID? = null) {
    companion object {
        val memory = HashMap<UUID, YamlConfiguration>()

        val size: Int
            get() {
                var r = 0
                Paths.get("plugins", "tag").toFile().listFiles().forEach {
                    if (!it.isHidden && !it.isDirectory) {
                        if (YamlConfiguration.loadConfiguration(it)?.isSet("password") == true) r++
                    }
                }
                r += Paths.get("plugins", "tag", "preregister").toFile().listFiles()?.size ?: 0
                return r
            }
    }

    abstract val tagFile: File
    private var privateTag: YamlConfiguration? = null
    var tag: YamlConfiguration
        get() = if (uuid != null) {
            memory[uuid!!].let {
                if (it == null) {
                    val r = initTag(createNew)
                    tag = r
                    r
                } else it
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
            if (uuid != null) memory[uuid!!] = value
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
        YamlConfiguration.loadConfiguration(
            fixTag(
                tagFile, createNew
            )
        )
    } catch (e: Throwable) {
        println("Could not load tag for player $uuid")
        throw e
    }

    private fun fixTag(file: File, createNew: Boolean): Reader {
        if (!file.exists()) {
            if (createNew) {
                file.createNewFile()
                return file.reader()
            } else throw FileNotFoundException("Cannot locate tag file at ${file.absolutePath}.")
        }
        var text = file.readText()
        val old = listOf("com.zhufu.opencraft.manager.BlockLockManager\$XZ")
        val new = listOf("com.zhufu.opencraft.BlockLockManager\$XZ")
        old.forEachIndexed { index, s ->
            if (text.contains(s)) {
                text = text.replace(s, new[index])
            }
        }
        return StringReader(text)
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
        set(value) {
            tag.set("password", value)
            saveTag()
        }

    var currency: Long
        get() = tag.getLong("currency")
        set(value) = tag.set("currency", value)
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
    var nickname: String
        get() = tag.getString("nickname", name)!!
        set(value) = tag.set("nickname", value)

    val offlinePlayer: OfflinePlayer
        get() = if (uuid != null) Bukkit.getOfflinePlayer(uuid?:throw IllegalStateException()) else throw IllegalStateException("Cannot read offline player for an info with uuid null.")

    var name: String?
        get() = tag.getString("name")
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

    val statics get() = PlayerStatics.from(this)
    val messagePool = MessagePool.from(tag)
}