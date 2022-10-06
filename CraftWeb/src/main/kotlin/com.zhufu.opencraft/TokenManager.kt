package com.zhufu.opencraft

import com.zhufu.opencraft.data.WebInfo
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.concurrent.timer

data class Token(val uuid: UUID, val expire: Date, var user: WebInfo? = null)

class TokenManager {
    private val tokens = hashMapOf<UUID, Token>()

    fun acquire(expire: Duration = Duration.ofHours(1)): Token {
        val id = UUID.randomUUID()
        // token expires in 1 hour by default
        val later = Date.from(Instant.now() + expire)
        val token = Token(uuid = id, expire = later)
        tokens[id] = token
        timer(startAt = later, period = Duration.ofDays(1).toMillis()) {
            tokens.remove(id)
            cancel()
        }

        return token
    }

    /**
     * Get an **valid** token using its [uuid].
     */
    operator fun get(uuid: UUID) = tokens[uuid]

    /**
     * Make a token with [uuid] expire.
     */
    operator fun minusAssign(uuid: UUID) {
        tokens.remove(uuid)
    }
}