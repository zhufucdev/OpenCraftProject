package com.zhufu.opencraft

import com.zhufu.opencraft.data.OfflineInfo
import com.zhufu.opencraft.data.PreregisteredInfo
import com.zhufu.opencraft.data.RegisteredInfo
import com.zhufu.opencraft.data.WebInfo
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import java.util.*

val tokenManager = TokenManager()

@Serializable
data class User(val id: String, val uuid: String)

@Serializable
data class LoginResult(val success: Boolean, val user: User?)

@Serializable
data class TokenResult(val token: String, val spoil: Long)

@Serializable
data class LoginRequest(val id: String, val pwd: String, val token: String)

fun Routing.api() {
    loginHandler()
    tokenAcquire()
    heartbeat()
}

private fun Routing.loginHandler() {
    post("/login") {
        val req = call.receive<LoginRequest>()
        val token = tokenManager[UUID.fromString(req.token)]

        if (token == null) {
            call.respond(HttpStatusCode.Forbidden, "No token found.")
            return@post
        }

        if (token.user != null) {
            call.respond(HttpStatusCode.MethodNotAllowed, "Already logged in.")
            return@post
        }

        val info: WebInfo =
            if (PreregisteredInfo.exists(req.id)) {
                PreregisteredInfo(req.id)
            } else {
                val offlineInfo = OfflineInfo.findByName(req.id)
                if (offlineInfo == null) {
                    call.respond(HttpStatusCode.NotFound, "Player ${req.id} not found.")
                    return@post
                }

                RegisteredInfo(offlineInfo.uuid)
            }
        if (info.matchPassword(req.pwd)) {
            token.user = info
            call.respond(
                LoginResult(
                    success = true,
                    user = User(req.id, info.uuid.toString())
                )
            )
        } else {
            call.respond(HttpStatusCode.Unauthorized, "Password and ID didn't match.")
        }
    }
}

private fun Routing.tokenAcquire() {
    get("/token_acquire") {
        val token = tokenManager.acquire()
        call.respond(
            TokenResult(
                token = token.uuid.toString(),
                spoil = token.spoil.toInstant().toGMTDate().timestamp
            )
        )
    }
}

private fun Routing.heartbeat() {
    get("/heartbeat") {
        call.respond("Alive")
    }
}
