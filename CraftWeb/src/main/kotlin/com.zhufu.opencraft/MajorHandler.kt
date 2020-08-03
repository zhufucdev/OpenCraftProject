package com.zhufu.opencraft

import com.sun.net.httpserver.HttpExchange
import com.zhufu.opencraft.CraftWeb.Companion.logger
import com.zhufu.opencraft.WebInfo.Companion.users
import okhttp3.HttpUrl
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import java.io.InputStream
import java.net.InetAddress

class MajorHandler(private val conf: FileConfiguration) :
    MyHandler(logger) {

    override fun handleWithExceptions(exchange: HttpExchange) {
        val require = exchange.requestURI.path.removePrefix("/")

        val url = HttpUrl.get("https://" + conf.getString("hostName") + exchange.requestURI)
        response(content = byteArrayOf(), e = exchange, respondCode = 503)
    }

    companion object {
        fun response(content: ByteArray, e: HttpExchange, type: String = "text/plain", respondCode: Int = 206) {
            e.responseHeaders.apply {
                add("Access-Control-Allow-Origin", "*")
                set("Content-Type", type)
            }
            e.sendResponseHeaders(respondCode, content.size.toLong())
            e.responseBody.apply {
                write(content)
                close()
            }
        }

        fun response(content: InputStream, size: Long, e: HttpExchange, type: String = "text/plain") {
            e.responseHeaders.apply {
                add("Access-Control-Allow-Origin", "*")
                set("Content-Type", type)
            }
            e.sendResponseHeaders(200, size)
            e.responseBody.apply {
                content.copyTo(this)
                close()
            }
        }

        fun redirect(where: String, e: HttpExchange) {
            logger.info("Redirecting ${e.remoteAddress.hostName} to $where")
            response("<script>window.location.href=\"$where\"</script>".toByteArray(), e, MimeType.HTML.toString())
        }
    }

    private fun createInfo(name: String): WebInfo? {
        return users.values.firstOrNull { it.name == name }
            ?: if (PreregisteredInfo.exists(name)) {
                PreregisteredInfo(name)
            } else {
                val uuid = Bukkit.getOfflinePlayer(name).uniqueId
                if (RegisteredInfo.exists(uuid))
                    RegisteredInfo(uuid)
                else null
            }?.also {
                PlayerManager.add(it)
            }
    }

    private fun register(name: String, address: InetAddress): WebInfo {
        return PreregisteredInfo(name).also {
            users[address] = it
        }
    }

    private fun containsUser(address: InetAddress) = users.containsKey(address)

}