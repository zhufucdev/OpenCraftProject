package com.baidu.translate

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import java.time.Duration
import java.util.StringJoiner
import java.util.concurrent.TimeoutException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.concurrent.thread

object HTTPUtil {
    fun request(url: String?, timeout: Duration = Duration.ofSeconds(3)): String {
        var response = ""
        val fetchThread = thread {
            var connection: HttpURLConnection? = null
            response = try {
                val url1 = URL(url)
                connection = url1.openConnection() as HttpURLConnection
                if (connection is HttpsURLConnection) {
                    val ssl = SSLContext.getInstance("TLS")
                    ssl.init(null, arrayOf(skip), null)
                    connection.sslSocketFactory = ssl.socketFactory
                }
                connection.requestMethod = "GET"
                val sb = StringBuilder()
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line)
                        sb.append(System.lineSeparator())
                    }
                }
                sb.toString()
            } finally {
                connection?.disconnect()
            }
        }
        fetchThread.join(timeout.toMillis())
        return response
    }

    private val skip = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate>? {
            return null
        }
    }
}