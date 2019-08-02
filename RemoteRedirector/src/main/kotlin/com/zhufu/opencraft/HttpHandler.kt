package com.zhufu.opencraft

import com.sun.net.httpserver.HttpExchange
import okhttp3.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class HttpHandler(private val remoteHost: String, private val isHttps: Boolean) : MyHandler() {
    private val client = OkHttpClient.Builder()
        .sslSocketFactory(ssl.socketFactory, xtm)
        .hostnameVerifier(doNotVerifier)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    override fun handleWithExceptions(exchange: HttpExchange) {
        println("Request received from ${exchange.remoteAddress.hostName}.")

        val url =
            with("${if (isHttps) "https:" else "http:"}$remoteHost${exchange.requestURI}") {
                HttpUrl.get(this).newBuilder()
                    .addQueryParameter("hostName", exchange.remoteAddress.hostName)
                    .build()
            }
        val request = Request.Builder()
            .url(url)
        if (exchange.requestMethod == "POST") {
            println("Posting data to $url")
            request.post(
                RequestBody.create(
                    MediaType.parse(exchange.requestHeaders.getFirst("Content-Type")),
                    exchange.requestBody.readBytes()
                )
            )
        } else {
            println("Getting data from $url")
            request.get()
        }
        val response =
            client.newCall(request.build())
                .execute()
        with(response) {
            headers().names().forEach {
                exchange.responseHeaders.add(it, header(it))
            }
            exchange.sendResponseHeaders(code(), body()?.contentLength() ?: 0)
            body()?.byteStream()?.copyTo(exchange.responseBody)
        }
        exchange.close()
    }

    companion object {
        val xtm = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}

            override fun getAcceptedIssuers(): Array<X509Certificate?> {
                return emptyArray()
            }
        }
        val ssl = SSLContext.getInstance("SSL").apply {
            init(null, arrayOf(xtm), SecureRandom())
        }
        val doNotVerifier = HostnameVerifier { _, _ -> true }
    }
}