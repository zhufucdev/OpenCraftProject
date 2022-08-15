package com.zhufu.opencraft.player_community

import com.mongodb.client.model.Filters
import com.zhufu.opencraft.Base
import com.zhufu.opencraft.api.ChatInfo
import com.zhufu.opencraft.data.Database
import com.zhufu.opencraft.data.DatabaseRecord
import com.zhufu.opencraft.data.RecordHolder
import com.zhufu.opencraft.data.ServerPlayer
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toSuccessMessage
import com.zhufu.opencraft.util.toTipMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bson.Document
import java.text.SimpleDateFormat
import java.util.*

open class MessagePool internal constructor(val owner: UUID) : RecordHolder<Message> {
    enum class Type {
        Friend, System, Public, OneTime
    }

    protected val collection = Database.messagePool(owner)
    fun add(text: String, type: Type): Message {
        val max = collection.find().maxOfOrNull { it.getInteger("_id") } ?: -1
        val msg = Message(text, false, max + 1, type, null, this)
        collection.insertOne(msg.toDocument())
        return msg
    }

    fun remove(id: Int) {
        collection.deleteOne(Filters.eq(id))
    }
    fun forEach(l: ((Message) -> Unit)) {
        collection.find().sortedBy { it.getInteger("_id") }.forEach { l(Message.from(it, this)) }
    }

    override fun update(record: Message) {
        collection.replaceOne(Filters.eq(record.id), record.toDocument())
    }

    operator fun get(id: Int): Message? {
        return Message.from(collection.find(Filters.eq(id)).first() ?: return null, this)
    }

    open fun sendTo(player: ChatInfo, msg: Message) {
        player.playerOutputStream.send(msg.toComponent(player))
        if (msg.type == Type.OneTime) {
            collection.deleteOne(Filters.eq(msg.id))
        }
    }

    open fun sendAllTo(player: ChatInfo) {
        PublicMessagePool.sendAllTo(player)
        forEach {
            sendTo(player, it)
        }
    }

    open fun sendUnreadTo(player: ChatInfo) {
        forEach {
            if (!it.read) {
                sendTo(player, it)
            }
        }
    }

    private fun markRead(id: Int, read: Boolean): Boolean {
        val doc = collection.find(Filters.eq(id)).first() ?: return false
        doc["read"] = read
        collection.replaceOne(Filters.eq(id), doc)
        return true
    }

    open fun markAsRead(id: Int) = markRead(id, true)

    open fun markAsUnread(id: Int) = markRead(id, false)

    val isEmpty get() = collection.find().first() == null

    companion object {
        private val cache = HashMap<ServerPlayer, MessagePool>()
        fun of(who: ServerPlayer): MessagePool {
            if (cache.containsKey(who))
                return cache[who]!!
            val r = MessagePool(who.uuid)
            cache[who] = r
            return r
        }

        fun remove(who: ServerPlayer) = cache.remove(who)
    }
}

object PublicMessagePool : MessagePool(Base.serverID) {
    override fun markAsRead(id: Int) =
        throw UnsupportedOperationException("Call [markAsRead(Int,ServerPlayer)] instead.")

    override fun sendAllTo(player: ChatInfo) {
        forEach {
            sendTo(player, it)
        }
    }

    override fun sendUnreadTo(player: ChatInfo) {
        forEach {
            if (!it.extra.containsKey(player.uuid.toString())) {
                sendTo(player, it)
            }
        }
    }

    override fun sendTo(player: ChatInfo, msg: Message) {
        player.playerOutputStream.send(msg.toComponent(player))
        if (msg.type == Type.OneTime) {
            msg.apply {
                if (!extra.containsKey(player.uuid.toString())) {
                    extra[player.uuid.toString()] = true
                }
            }
        }
    }

    fun markAsRead(id: Int, player: ServerPlayer): Boolean {
        var r = false
        get(id)?.apply {
            if (!extra.containsKey(player.uuid.toString())) {
                extra[player.uuid.toString()] = true
                r = true
            }
        }
        return r
    }

    fun markAsUnread(id: Int, player: ServerPlayer): Boolean {
        var r = false
        get(id)?.apply {
            if (extra.containsKey(player.uuid.toString())) {
                extra[player.uuid.toString()] = null
                r = true
            }
        }
        return r
    }

    fun isRead(id: Int, player: ServerPlayer) = get(id)?.extra?.contains(player.uuid.toString())
}

class Message internal constructor(
    val text: String,
    var read: Boolean,
    val id: Int,
    val type: MessagePool.Type,
    extra: Document? = null,
    override val parent: MessagePool
) : DatabaseRecord<Message> {
    fun sendTo(receiver: ChatInfo) = parent.sendTo(receiver, this)

    val extra: Document by lazy { extra ?: Document() }

    fun recordTime() {
        extra["time"] = System.currentTimeMillis()
        parent.update(this)
    }

    val time get() = extra.getLong("time") ?: -1L
    var sender: UUID?
        get() = extra.get("sender", UUID::class.java)
        set(value) {
            extra["sender"] = value
            parent.update(this)
        }

    @Suppress("UNCHECKED_CAST")
    fun toComponent(player: ChatInfo): Component {
        return Component.empty().toBuilder().apply {
            // time & date prefix
            if (time >= 0) {
                val simple = SimpleDateFormat("MM/dd HH:mm").format(Date(time))
                val detailed = SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Date(time))
                it.append(
                    Component.text("[$simple] ")
                        .hoverEvent {
                            HoverEvent.showText(Component.text(detailed))
                                    as HoverEvent<Any>
                        }
                )
            }
            if (sender != null) {
                it.append(Component.text(ServerPlayer.of(uuid = sender).name + ": "))
            }
            // main content
            it.append(Component.text(TextUtil.getCustomizedText(text, player) + ' '))
            // [Read] label
            val tip = Language[player.targetLang, "msg.clickToRead"].toTipMessage()
            val command = "/pu server:markMessageRead ${id}${if (type == MessagePool.Type.Public) " public" else ""}"
            val readLabel = "[${Language.byChat(player, "msg.read")}]".toSuccessMessage()
            it.append(
                readLabel
                    .clickEvent(ClickEvent.runCommand(command))
                    .hoverEvent { HoverEvent.showText(tip) as HoverEvent<Any> }
            )
        }.build()
    }

    override fun toDocument(): Document = Document(mapOf(
        "_id" to id,
        "text" to text,
        "read" to read,
        "type" to type.name,
        "extra" to extra
    ))

    override fun equals(other: Any?): Boolean =
        other is Message
                && other.text == text
                && other.id == id

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + read.hashCode()
        result = 31 * result + id
        result = 31 * result + type.hashCode()
        result = 31 * result + extra.hashCode()
        return result
    }

    companion object {
        internal fun from(doc: Document, parent: MessagePool) = Message(
            doc.getString("text"),
            doc.getBoolean("read"),
            doc.getInteger("_id"),
            MessagePool.Type.valueOf(doc.getString("type")),
            doc["extra"] as Document?,
            parent
        )
    }
}
