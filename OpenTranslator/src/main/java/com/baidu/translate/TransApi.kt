package com.baidu.translate

import java.net.URLEncoder

class TransApi(private val appid: String, private val securityKey: String) {
    fun getTransResult(query: String, from: String, to: String): String? {
        val url = buildParams(query, from, to)
        return try {
            HTTPUtil.request(url)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildParams(query: String, from: String, to: String): String {
        val salt = System.currentTimeMillis().toString()
        val src = appid + query + salt + securityKey
        return ("https://" + TRANS_API_HOST + "/" + TRANS_API_PATHS + "?q=" + URLEncoder.encode(query, Charsets.UTF_8)
                + "&from=" + from + "&to=" + to + "&appid=" + appid + "&salt=" + salt + "&sign=" + MD5.md5(src))
    }

    companion object {
        private const val TRANS_API_HOST = "fanyi-api.baidu.com"
        private const val TRANS_API_PATHS = "api/trans/vip/translate"
    }
}