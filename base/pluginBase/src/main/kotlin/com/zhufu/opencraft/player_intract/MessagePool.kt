package com.zhufu.opencraft.player_intract

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
import com.zhufu.opencraft.*
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.StringWriter
import java.util.*

open class MessagePool private constructor() {
    enum class Type {
        Friend, System, Public
    }

    class PublicMessagePool : MessagePool() {
        override fun markAsRead(i: Int) =
            throw UnsupportedOperationException("Call #markAsRead(Int,ServerPlayer) instead.")

        override fun sendAllTo(player: ChatInfo) {
            messages.forEach {
                sendTo(player, it)
            }
        }

        override fun sendUnreadTo(player: ChatInfo) {
            messages.forEach {
                if (it.extra?.contains(player.id) != true) {
                    sendTo(player, it)
                }
            }
        }

        fun markAsRead(id: Int, player: ServerPlayer): Boolean {
            var r = false
            get(id)?.apply {
                if (extra == null) extra = YamlConfiguration()
                if (!extra!!.contains(player.name!!)) {
                    extra!!.set(player.name!!, "R")
                    r = true
                }
            }
            return r
        }

        fun markAsUnread(id: Int, player: ServerPlayer): Boolean {
            var r = false
            get(id)?.apply {
                if (extra != null) {
                    if (extra!!.contains(player.name!!)) {
                        extra!!.set(player.name!!, null)
                        r = true
                    }
                } else r = true
            }
            return r
        }
        fun isRead(id: Int,player: ServerPlayer) = get(id)?.extra?.contains(player.name!!)
    }

    class Message(
        val text: String,
        var read: Boolean,
        val id: Int,
        val type: Type,
        var extra: ConfigurationSection? = null
    )

    internal val messages = ArrayList<Message>()
    fun add(text: String, type: Type, extra: ConfigurationSection? = null) {
        val max = messages.maxBy { it.id }?.id ?: -1
        messages.add(Message(text, false, max + 1, type, extra))
    }

    fun remove(id: Int) = messages.removeAll { it.id == id }
    fun forEach(l: ((Message) -> Unit)) {
        messages.sortBy { it.id }
        messages.forEach(l)
    }
    operator fun get(id: Int) = messages.firstOrNull { it.id == id }

    private fun getJson(msg: Message, player: ChatInfo): JsonElement {
        val sr = StringWriter()
        JsonWriter(sr)
            .beginArray()
            .value(TextUtil.getCustomizedText(msg.text, player) + ' ')
            .beginObject()//Click to read and hover for help.
            .name("text")
            .value(TextUtil.getColoredText("[${Language.byChat(player, "msg.read")}]", TextUtil.TextColor.GREEN, true))
            .name("clickEvent")
            .beginObject()
            .name("action").value("run_command")
            .name("value")
            .value("/pu server:markMessageRead ${msg.id}${if (msg.type == Type.Public) " public" else ""}")
            .endObject()
            .name("hoverEvent")
            .beginObject()
            .name("action").value("show_text")
            .name("value").value(TextUtil.tip(Language[player.targetLang, "msg.clickToRead"]))
            .endObject()
            .endObject()
            .endArray()
        return JsonParser().parse(sr.toString())
    }

    internal fun sendTo(player: ChatInfo, msg: Message) = player.playerStream.sendRaw(getJson(msg, player))

    open fun sendAllTo(player: ChatInfo) {
        Base.publicMsgPool.sendAllTo(player)
        messages.forEach {
            sendTo(player, it)
        }
    }

    open fun sendUnreadTo(player: ChatInfo) {
        messages.forEach {
            if (!it.read) {
                sendTo(player, it)
            }
        }
    }

    open fun markAsRead(i: Int) = let {
        var r = false
        messages.forEach {
            if (it.id == i && !it.read) {
                r = true
                it.read = true
            }
        }
        r
    }

    open fun markAsUnread(i: Int) = let {
        var r = false
        messages.forEach {
            if (it.id == i && it.read) {
                r = true
                it.read = false
            }
        }
        r
    }

    fun serialize(): YamlConfiguration = YamlConfiguration().apply {
        messages.forEach {
            createSection(it.id.toString()).apply {
                set("msg", it.text)
                set("read", it.read)
                set("type", it.type.name)
                if (it.extra?.getKeys(false)?.isNotEmpty() == true)
                    set("extra", it.extra)
            }
        }
    }

    val isEmpty get() = messages.isEmpty()

    companion object {
        private fun addAllTo(r: MessagePool, section: ConfigurationSection) {
            var max = 0
            section.getKeys(false).sorted().forEach {
                r.messages.add(
                    Message(
                        text = section.getString("$it.msg", "")!!,
                        read = section.getBoolean("$it.read", false),
                        id = it.toIntOrNull().let { id -> id?.also { if (max < id) max = id } ?: max++ },
                        type = Type.valueOf(section.getString("$it.type", "System")!!),
                        extra = section.getConfigurationSection("$it.extra")
                    )
                )
            }
        }

        fun from(tag: YamlConfiguration): MessagePool {
            val r = MessagePool()
            val section = tag.getConfigurationSection("messages")
            if (section != null) addAllTo(r, section)

            return r
        }

        fun public(file: File): PublicMessagePool {
            val r = PublicMessagePool()
            if (!file.exists()) {
                if (!file.parentFile.exists())
                    file.parentFile.mkdirs()
                file.createNewFile()
            } else
                addAllTo(r, YamlConfiguration.loadConfiguration(file))
            return r
        }
    }
}