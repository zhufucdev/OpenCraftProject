package com.zhufu.opencraft.headers.util

import java.io.File
import java.nio.charset.Charset

@Suppress("unused")
object FileUtils {
    fun of(path: String) = File(path)
    fun writeText(path: String, text: String, encode: String) = of(path).writeText(text, Charset.forName(encode))
    fun writeText(path: String, text: String) = writeText(path, text, "UTF-8")
    fun inputStream(path: String) = of(path).inputStream()
    fun inputStream(file: File) = file.inputStream()
}