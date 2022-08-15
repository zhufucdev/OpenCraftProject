package com.zhufu.opencraft.player_community

import com.zhufu.opencraft.data.Database
import com.zhufu.opencraft.data.ServerPlayer
import java.util.*
import javax.security.auth.Destroyable

class Friendships private constructor(val owner: ServerPlayer) : Iterable<Friendship>, Destroyable {
    fun contains(who: ServerPlayer) = any { it.friend == who }
    operator fun get(name: String) = firstOrNull { it.name == name }
    operator fun get(who: ServerPlayer) = firstOrNull { it.friend == who }

    fun add(who: ServerPlayer): Friendship {
        return if (!contains(who)) {
            val impl =
                FriendshipImpl.between(owner, who)
                    ?.also { it.startAt = System.currentTimeMillis() } // accept existing
                    ?: FriendshipImpl(owner, who)
            Database.friendship(impl.id, impl.doc)
            val r = Friendship(impl, owner)
            onChanged?.invoke(r)
            r
        } else {
            this[who]!!
        }
    }

    fun remove(who: ServerPlayer): Boolean {
        return Database.friendship.deleteOne(Database.friendshipInvolving(who.uuid, owner.uuid))
            .let {
                it.deletedCount > 0
            }
    }

    private var onChanged: ((Friendship) -> Unit)? = null
    fun setOnChangedListener(l: (Friendship) -> Unit) {
        onChanged = l
    }

    private var isDestroyed = false
    override fun isDestroyed(): Boolean = isDestroyed
    override fun destroy() {
        forEach {
            it.destroy()
        }
        cache.remove(owner)
        isDestroyed = true
    }

    companion object {
        private val cache = HashMap<ServerPlayer, Friendships>()

        fun of(player: ServerPlayer) =
            cache[player]
                ?: Friendships(player).also { cache[player] = it }
    }

    override fun iterator() = Database.friendship(owner).map {
        Friendship(FriendshipImpl.deserialize(it), owner)
    }.iterator()
}