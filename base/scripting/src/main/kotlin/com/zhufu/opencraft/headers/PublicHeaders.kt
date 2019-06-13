package com.zhufu.opencraft.headers

import com.zhufu.opencraft.Header
import com.zhufu.opencraft.headers.util.Utils
import org.bukkit.ChatColor
import java.util.function.Function

object PublicHeaders : Header {
    override val members: List<Pair<String, Any?>>
        get() = listOf(
            "util" to Utils,
            "getColor" to Function<String, String> { name -> ChatColor.valueOf(name.toUpperCase()).toString() },
            "allColors" to Function<Nothing, Array<ChatColor>> { ChatColor.values() }
        )
}