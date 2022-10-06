package com.zhufu.opencraft

import com.google.gson.JsonParser
import com.zhufu.opencraft.data.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import java.time.Duration
import java.util.*
import javax.imageio.ImageIO
import kotlin.concurrent.fixedRateTimer

fun Routing.api() {
    loginHandler()
    registerHandler()
    tokenAcquire()
    avatarHandler()
    logoutHandler()
    userHandler()
    heartbeat()
}

val tokenManager = TokenManager()

/* Results */
@Serializable
data class User(val id: String, val nickname: String?, val uuid: String, val avatar: String?) {
    companion object {
        fun of(info: WebInfo) =
            User(
                id = info.name!!,
                uuid = info.uuid.toString(),
                avatar = if (info.face.exists()) "/avatar/${info.uuid}.jpg" else null,
                nickname = info.nickname
            )
    }
}

@Serializable
data class LoginResult(val success: Boolean, val user: User?)
@Serializable
data class LogoutResult(val success: Boolean)

@Serializable
data class RegisterResult(val success: Boolean, val user: User?)

@Serializable
data class TokenResult(val token: String, val spoil: Long)

/* Requests */
@Serializable
data class TokenRequest(val remote: String)

@Serializable
data class LoginRequest(val id: String, val pwd: String, val token: String)

@Serializable
data class RequestWithToken(val token: String)

@Serializable
data class RegisterRequest(val id: String, val pwd: String, val token: String, val captchaToken: String)

@Serializable
data class AvatarChangeRequest(val image: String, val token: String)

private fun Routing.loginHandler() {
    post("/login") {
        val req = call.receive<LoginRequest>()
        val token = tokenManager[UUID.fromString(req.token)]

        if (token == null) {
            call.respond(HttpStatusCode.Forbidden, "Token expired.")
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
            call.respond(LoginResult(true, User.of(info)))
        } else {
            call.respond(HttpStatusCode.Unauthorized, "Password and ID didn't match.")
        }
    }
}

private fun Routing.logoutHandler() {
    post("/logout") {
        val req = call.receive<RequestWithToken>()
        val token = tokenManager[UUID.fromString(req.token)]

        if (token == null) {
            call.respond(HttpStatusCode.Forbidden, "Token expired.")
            return@post
        }

        if (token.user != null) {
            token.user = null
            call.respond(LogoutResult(true))
            return@post
        }

        call.respond(LogoutResult(false))
    }
}

private fun Routing.registerHandler() {
    val format = Regex("[a-zA-Z_0-9]{3,15}")

    post("/register") {
        val req = call.receive<RegisterRequest>()
        val token = tokenManager[UUID.fromString(req.token)]

        // request validation check
        if (token == null) {
            call.respond(HttpStatusCode.Forbidden, "Token expired.")
            return@post
        }
        val httpCall =
            OkHttpClient()
                .newCall(
                    okhttp3.Request.Builder()
                        .url("https://recaptcha.net/recaptcha/api/siteverify")
                        .post(
                            okhttp3.FormBody.Builder()
                                .add("secret", CraftWeb.instance.config.getString("reCAPTCHASecret")!!)
                                .add("response", req.captchaToken)
                                .build()
                        )
                        .build()
                )
        val verificationResponse = httpCall.execute()
        if (!verificationResponse.isSuccessful) {
            call.respond(HttpStatusCode.InternalServerError, "reCAPTCHA service unavailable.")
            return@post
        }
        val verificationResult = JsonParser.parseReader(verificationResponse.body!!.charStream()).asJsonObject
        if (!verificationResult["success"].asBoolean) {
            call.respond(HttpStatusCode.Forbidden, "CAPTCHA unsolved.")
            return@post
        }

        // form validation check
        if (!format.matches(req.id)) {
            call.respond(RegisterResult(false, null))
            return@post
        }
        try {
            ServerPlayer.of(name = req.id)
            call.respond(RegisterResult(false, null))
            return@post
        } catch (e: Exception) {
            // continue
        }

        val info = PreregisteredInfo(req.id)
        info.setPassword(req.pwd)
        call.respond(RegisterResult(true, User.of(info)))
    }
}

private fun Routing.avatarHandler() {
    get("/avatar/{id}") {
        val id = call.parameters["id"]!!
        val info = WebInfo.of(UUID.fromString(id))
        if (info.face.exists()) {
            call.response.header(
                HttpHeaders.ContentDisposition,
                "$id.jpg"
            )
            call.respondFile(info.face)
        } else {
            call.respond(HttpStatusCode.NotFound, "No avatar for $id")
        }
    }

    post("/avatar") {
        val req = call.receive<AvatarChangeRequest>()
        val token = tokenManager[UUID.fromString(req.token)]

        if (token == null) {
            call.respond(HttpStatusCode.Forbidden, "Token invalid.")
            return@post
        }

        val info = token.user
        if (info == null) {
            call.respond(HttpStatusCode.Forbidden, "Not logged in.")
            return@post
        }

        val content = Base64.getDecoder().decode(req.image)
        withContext(Dispatchers.IO) {
            val reader = content.inputStream()
            val image = ImageIO.read(reader)
            reader.close()

            ImageIO.write(scaleAvatar(image), "jpg", info.face)
        }
    }
}

private fun Routing.userHandler() {
    get("/user/{id}") {
        val id = UUID.fromString(call.parameters["id"])
        val info = WebInfo.of(id)
        call.respond(User.of(info))
    }
}

private fun Routing.tokenAcquire() {
    val remotes = hashMapOf<String, Int>()

    post("/token_acquire/{action?}") {
        val remote = call.receive<TokenRequest>().remote
        val callsByRemote = remotes[remote]
        if (callsByRemote != null) {
            if (callsByRemote > 100) {
                call.respond(HttpStatusCode.TooManyRequests, "Token acquirement limit reached.")
                return@post
            } else {
                remotes[remote] = callsByRemote + 1
            }
        } else {
            remotes[remote] = 1
        }

        val token = when (call.parameters["action"]) {
            null -> {
                // default session token expires in 1 hour
                tokenManager.acquire()
            }
            // other types to be added
            else -> {
                call.respond(HttpStatusCode.BadRequest, "Action not supported.")
                return@post
            }
        }

        call.respond(
            TokenResult(
                token = token.uuid.toString(),
                spoil = token.expire.toInstant().toGMTDate().timestamp
            )
        )
    }

    // Timer to reset call limit per hour
    fixedRateTimer(name = "call-limit-reset", period = Duration.ofHours(1).toMillis()) {
        remotes.clear()
    }
}

private fun Routing.heartbeat() {
    get("/heartbeat") {
        call.respond("Alive")
    }
}
