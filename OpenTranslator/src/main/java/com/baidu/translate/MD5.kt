package com.baidu.translate

import org.jetbrains.annotations.Contract
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object MD5 {
    private val hexDigits = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
        'e', 'f'
    )

    fun md5(input: String?): String? {
        return if (input == null) null else try {
            val messageDigest = MessageDigest.getInstance("MD5")
            val inputByteArray = input.toByteArray(StandardCharsets.UTF_8)
            messageDigest.update(inputByteArray)
            val resultByteArray = messageDigest.digest()
            byteArrayToHex(resultByteArray)
        } catch (e: NoSuchAlgorithmException) {
            null
        }
    }

    @Contract("_ -> new")
    private fun byteArrayToHex(byteArray: ByteArray): String {
        val resultCharArray = CharArray(byteArray.size * 2)
        var index = 0
        for (b in byteArray) {
            resultCharArray[index++] = hexDigits[b.toInt() ushr 4 and 0xf]
            resultCharArray[index++] = hexDigits[b.toInt() and 0xf]
        }
        return String(resultCharArray)
    }
}