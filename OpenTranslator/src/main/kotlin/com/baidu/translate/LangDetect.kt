package com.baidu.translate

import java.net.URLEncoder

object LangDetect {
    private const val DETECT_API_HOST = "fanyi.baidu.com"
    private const val DETECT_API_PATH = "langdetect"
    const val langUnknown = "unknown"
    fun detectLanguage(text: String): String {
        return try {
            val encoded = URLEncoder.encode(text, Charsets.UTF_8)
            HTTPUtil.request("https://$DETECT_API_HOST/$DETECT_API_PATH?query=$encoded")
        } catch (e: Exception) {
            langUnknown
        }
    }
}