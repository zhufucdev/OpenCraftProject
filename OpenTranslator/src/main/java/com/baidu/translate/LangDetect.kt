package com.baidu.translate

object LangDetect {
    private const val DETECT_API_HOST = "fanyi.baidu.com"
    private const val DETECT_API_PATH = "langdetect"
    const val langUnknown = "unknown"
    fun detectLanguage(text: String): String? {
        return try {
            HTTPUtil.request("https://$DETECT_API_HOST/$DETECT_API_PATH?query=$text")
        } catch (e: Exception) {
            langUnknown
        }
    }
}