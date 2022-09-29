package com.zhufu.opencraft

import com.zhufu.opencraft.data.WebInfo
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.concurrent.timer

data class Token(val uuid: UUID, val spoil: Date, var user: WebInfo? = null)

class TokenManager {
    private val tokens = hashMapOf<UUID, Token>()

    fun acquire(): Token {
        val id = UUID.randomUUID()
        // token invalidates after 3 hour
        val later = Date.from(Instant.now() + Duration.ofHours(3))
        val token = Token(uuid = id, spoil = later)
        tokens[id] = token
        timer(startAt = later, period = Duration.ofDays(1).toMillis()) {
            tokens.remove(id)
            cancel()
        }

        return token
    }

    operator fun get(uuid: UUID) = tokens[uuid]
}