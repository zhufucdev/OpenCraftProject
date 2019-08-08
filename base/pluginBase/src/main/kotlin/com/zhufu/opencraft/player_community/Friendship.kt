package com.zhufu.opencraft.player_community

import com.zhufu.opencraft.ServerPlayer
import org.bukkit.Bukkit
import java.util.*
import java.util.function.Predicate
import javax.security.auth.Destroyable
import kotlin.collections.HashMap

class Friendship private constructor(list: List<String>?, val parent: ServerPlayer) :
    ArrayList<FriendWrap>(), Destroyable {
    init {
        list?.forEach {
            try {
                add(FriendWrap(Friend.of(UUID.fromString(it)), parent))
            } catch (e: Exception) {
                Bukkit.getLogger().warning("Unable to initialize friendship for player ${parent.name}.")
                e.printStackTrace()
            }
        }
    }

    fun contains(name: String) = any { it.name == name }
    fun contains(who: ServerPlayer) = any { it.friend == who }
    operator fun get(name: String) = firstOrNull { it.name == name }
    operator fun get(who: ServerPlayer) = firstOrNull { it.friend == who }

    fun add(who: ServerPlayer, each: Boolean = true): FriendWrap {
        return if (!contains(who)) {
            val r = FriendWrap(Friend.from(parent, who) ?: Friend(parent, who, id = UUID.randomUUID()), parent)
            if (each) {
                if (who.friendship.contains(parent))
                    r.startAt = System.currentTimeMillis()
                else {
                    who.friendship.add(parent, false)
                }
            }
            add(r)
            r.save()
            onChanged?.invoke(r)
            r
        } else {
            this[who]!!
        }
    }

    override fun remove(element: FriendWrap): Boolean {
        return super.remove(element).also { if (it) {
            element.delete()
            onChanged?.invoke(element)
        } }
    }

    override fun removeAt(index: Int): FriendWrap {
        return super.removeAt(index).also {
            it.delete()
            onChanged?.invoke(it)
        }
    }

    fun remove(who: ServerPlayer): Boolean {
        for (i in this) {
            if (i.friend == who) {
                return remove(i).also {
                    if (it) onChanged?.invoke(i)
                }
            }
        }
        return false
    }

    override fun removeAll(elements: Collection<FriendWrap>): Boolean = throw UnsupportedOperationException()
    override fun removeRange(fromIndex: Int, toIndex: Int) = throw UnsupportedOperationException()

    fun save() {
        val list = arrayListOf<String>()
        forEach {
            it.save()
            list.add(it.id.toString())
        }
        parent.tag.apply {
            set("friendship", null)
            set("friendship", list.toList())
        }
    }

    private var onChanged: ((FriendWrap) -> Unit)? = null
    fun setOnChangedListener(l: (FriendWrap) -> Unit) {
        onChanged = l
    }

    fun firstOrNull(predicate: (FriendWrap) -> Boolean): FriendWrap? {
        for (i in this) {
            if (predicate(i)) return i
        }
        return null
    }

    private var isDestroyed = false
    override fun isDestroyed(): Boolean = isDestroyed
    override fun destroy() {
        forEach {
            it.destroy()
        }

        memory.remove(parent)
        isDestroyed = true
    }

    companion object {
        private val memory = HashMap<ServerPlayer, Friendship>()

        fun from(player: ServerPlayer) =
            memory[player]
                ?: Friendship(player.tag.getStringList("friendship"), player).also { memory[player] = it }
    }
}