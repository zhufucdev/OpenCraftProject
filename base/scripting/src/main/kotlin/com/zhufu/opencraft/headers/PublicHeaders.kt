package com.zhufu.opencraft.headers

import com.zhufu.opencraft.Header
import com.zhufu.opencraft.Language
import com.zhufu.opencraft.ServerPlayer
import com.zhufu.opencraft.headers.npc_wrap.NPCUtils
import com.zhufu.opencraft.headers.npc_wrap.BehaviorCollection
import com.zhufu.opencraft.headers.util.Utils
import com.zhufu.opencraft.script.AbstractScript
import org.bukkit.ChatColor
import java.util.function.Function
import java.util.function.LongFunction
import java.util.function.Supplier

class PublicHeaders(
    private val lang: String,
    private val script: AbstractScript,
    private val player: ServerPlayer? = null
) : Header {
    override val members: List<Pair<String, Any?>>
        get() = listOf(
            "util" to Utils,
            "getColor" to Function<String, String> { name -> ChatColor.valueOf(name.toUpperCase()).toString() },
            "getColors" to Supplier { ChatColor.values() },
            "delay" to LongFunction<Any?> { Thread.sleep(it) },
            "npc" to NPCUtils(Language.LangGetter(lang), script, player),
            "behavior" to Function<Any?,BehaviorCollection> { BehaviorCollection.deserialize(it, Language.LangGetter(lang)) }
        )

    override fun equals(other: Any?): Boolean =
        other is PublicHeaders
                && (other.lang == this.lang
                && ((this.player == null) || (this.player.uuid == other.player?.uuid)))

    override fun hashCode(): Int {
        return lang.hashCode() * 31 + (player?.uuid?.hashCode() ?: 0)
    }
}