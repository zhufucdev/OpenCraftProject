package com.zhufu.opencraft

import java.awt.image.BufferedImage
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