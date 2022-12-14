package com.zhufu.opencraft

import com.zhufu.opencraft.data.WebInfo
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.awt.image.BufferedImage
import java.util.*
import kotlin.math.roundToInt

const val AVATAR_SIZE = 400.0

fun scaleAvatar(image: BufferedImage): BufferedImage {
    val ratio = if (image.width > image.height) {
        AVATAR_SIZE / image.width
    } else {
        AVATAR_SIZE / image.height
    }
    val width = (image.width * ratio).roundToInt()
    val height = (image.height * ratio).roundToInt()
    val resized = BufferedImage(width, height, image.type)
    image.createGraphics().drawImage(resized, 0, 0, width, height, null)
    return resized
}

suspend fun ApplicationCall.getInfo(): WebInfo? {
    val req = receive<RequestWithToken>()
    val token = tokenManager[UUID.fromString(req.token)]

    if (token == null) {
        respond(HttpStatusCode.Forbidden, "Token expired.")
        return null
    }

    val info = token.user
    if (info == null) {
        respond(HttpStatusCode.Forbidden, "Not logged in.")
        return null
    }

    return info
}