package com.zhufu.opencraft

import java.io.File

enum class MimeType(private val string: String) {
    JSON("application/json"), JavaScript("text/javascript"), HTML("text/html"),
    CSS("text/css"), PlainText("text/plain"), PNG("image/png"),
    JPEG("image/jpeg"), ICO("image/vnd.microsoft.icon");

    override fun toString(): String = this.string
}

val File.mimeType: MimeType
    get() = when (extension) {
        "css" -> MimeType.CSS
        "js" -> MimeType.JavaScript
        "png" -> MimeType.PNG
        "jpg" -> MimeType.JPEG
        "html" -> MimeType.HTML
        else -> MimeType.PlainText
    }