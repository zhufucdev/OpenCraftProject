import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpExchange
import com.zhufu.opencraft.*
import com.zhufu.opencraft.CraftWeb.Companion.logger
import com.zhufu.opencraft.Game.env
import com.zhufu.opencraft.player_community.MessagePool
import okhttp3.HttpUrl
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import java.io.File
import java.net.InetAddress

class SimpleHandler(private val root: File, private val conf: FileConfiguration) : MyHandler() {
    val users = HashMap<InetAddress, WebInfo>()
    private val invalidFile: File
        get() = File(root, "invalidRequest.html")
    private val noSuchContentFile: File
        get() = File(root, "404.html")
    private val dpsed = HashMap<InetAddress, Long>()

    override fun handleWithExceptions(e: HttpExchange) {
        val require = e.requestURI.path.removePrefix("/")

        val url = HttpUrl.get("https://" + InetAddress.getLocalHost().hostAddress + e.requestURI)
        if (url == null) {
            response(invalidFile.readBytes(), e)
            return
        }
        val response: ByteArray = when {
            require.contains('/') -> File(root, require).let {
                if (it.exists())
                    it
                else
                    noSuchContentFile
            }.readBytes()
            require == "ui" -> {
                val request = url.queryParameter("request")
                if (request == "main") {
                    buildString {
                        append(
                            File(root, "introduction.html").readText()
                        )
                        append('\n')
                        append(
                            File(root, "footer.html").readText()
                        )
                    }.toByteArray()
                } else {
                    val whiteList = conf.getStringList("uiWhiteList")
                    if (whiteList.contains(request)) {
                        File(root, "$request.html")
                    } else {
                        invalidFile
                    }.readBytes()
                }
            }
            require == "user" -> {
                when (url.queryParameter("request")) {
                    "login" -> {
                        val obj = JsonObject()
                        val json = JsonParser().parse(e.requestBody.bufferedReader()).asJsonObject
                        if (json == null) {
                            obj.addProperty("r", -2)
                        } else {
                            val name = json["name"]?.asString
                            val pwd = json["password"]?.asString
                            if (name == null || pwd == null) {
                                response(invalidFile.readBytes(), e)
                                return
                            }

                            val info = createInfo(name)
                            obj.apply {
                                when {
                                    containsUser(e.remoteAddress.address) ->
                                        addProperty("r", 2)
                                    info == null -> //When no such user
                                        addProperty("r", -1)
                                    info.password != pwd -> //When wrong password
                                        addProperty("r", 1)
                                    else -> {
                                        //When success
                                        addProperty("r", 0)
                                        users[e.remoteAddress.address] = info
                                        logger.info("${e.remoteAddress.hostName} has log in.")
                                    }
                                }
                            }
                        }
                        obj.toString().toByteArray()
                    }
                    "logout" -> {
                        val obj = JsonObject()
                        obj.apply {
                            if (containsUser(e.remoteAddress.address)) {
                                users[e.remoteAddress.address]!!.saveTag()
                                users.remove(e.remoteAddress.address)
                                addProperty("r", 0)
                                logger.info("${e.remoteAddress.hostName} has log out.")
                            } else {
                                addProperty("r", -1)
                            }
                        }.toString().toByteArray()
                    }
                    "sign-up" -> {
                        val obj = JsonObject()
                        obj.apply {
                            if (containsUser(e.remoteAddress.address)) {
                                addProperty("r", 2)
                            } else {
                                val json = JsonParser().parse(e.requestBody.bufferedReader()).asJsonObject
                                if (json == null)
                                    addProperty("r", -2)
                                else {
                                    val id = json["name"]?.asString
                                    val pwd = json["password"]?.asString
                                    val nick = json["nickname"]?.asString
                                    if (id == null || pwd == null) {
                                        addProperty("r", -1)
                                    } else if (PreregisteredInfo.exist(id) || OfflineInfo.listPlayers { it.name == id }.isNotEmpty()) {
                                        addProperty("r", 2)
                                    } else {
                                        val register = register(id, e.remoteAddress.address)
                                        var excepted = false
                                        register.apply {
                                            password = pwd
                                            name = id
                                            if (nick != null) {
                                                if (nick.length <= 20)
                                                    nickname = nick
                                                else {
                                                    excepted = true
                                                    addProperty("r", 1)
                                                }
                                            }
                                            saveTag()
                                        }
                                        if (!excepted)
                                            addProperty("r", 0)
                                    }
                                }
                            }
                        }

                        obj.toString().toByteArray()
                    }
                    "destroy" -> {
                        val find = users[e.remoteAddress.address]
                        JsonObject().apply {
                            if (find == null) {
                                addProperty("r", -1)
                            } else {
                                find.tagFile.deleteRecursively()
                                users.remove(e.remoteAddress.address)
                                addProperty("r", 0)
                            }
                        }.toString().toByteArray()
                    }

                    "check" -> {
                        val obj = JsonObject()
                        val find = users[e.remoteAddress.address]
                        obj.apply {
                            if (find == null) {
                                addProperty("r", -1)
                            } else {
                                try {
                                    val check = ArrayList<String>()
                                    check.addAll(url.queryParameter("check")?.split(',') ?: listOf("all"))
                                    if (check.contains("face")) {
                                        if (find.face.exists()) {
                                            response(find.face.readBytes(), e)
                                        } else {
                                            response(ByteArray(0), e)
                                        }
                                        return
                                    }

                                    if (check.contains("uploadDone")) {
                                        File(find.face.path + ".tmp").also {
                                            obj.addProperty("r", if (it.exists()) {
                                                find.face.apply {
                                                    delete()
                                                    it.renameTo(this)
                                                    it.delete()
                                                }
                                                0
                                            } else -1)
                                            response(obj.toString().toByteArray(), e)
                                        }
                                        return
                                    }
                                    addProperty("r", 0)
                                    val doAll = check.contains("all")
                                    if (doAll || check.contains("ID"))
                                        addProperty("ID", find.name)
                                    if (doAll || check.contains("time"))
                                        addProperty("time", find.gameTime)
                                    if (doAll || check.contains("nickname"))
                                        addProperty("nickname", find.nickname ?: find.name)
                                    if (doAll || check.contains("coin"))
                                        addProperty("coin", find.currency)
                                    if (doAll || check.contains("lang"))
                                        addProperty("lang", find.userLanguage)
                                    if (doAll || check.contains("containsFace"))
                                        addProperty("face", find.face.exists())
                                    if (doAll || check.contains("isPreregister"))
                                        addProperty("isPreregister", find is PreregisteredInfo)
                                    if (doAll || check.contains("privileges")) {
                                        val r = JsonArray()
                                        if (find.isSurveyPassed)
                                            r.add("member")
                                        if (find.isBuilder)
                                            r.add("builder")
                                        if (r.size() > 0)
                                            add("privileges", r)
                                    }
                                    if (check.contains("statics")) {
                                        val statics = find.statics?.getData()
                                        if (statics == null)
                                            addProperty("r", -1)
                                        else
                                            add("statics", statics)
                                    }
                                    if (check.contains("message")) {
                                        val messages = JsonArray()
                                        find.messagePool.forEach {
                                            messages.add(
                                                JsonObject().apply {
                                                    addProperty("id", it.id)
                                                    addProperty("text", it.text)
                                                    addProperty("read", it.read)
                                                    addProperty("type", it.type.name.toLowerCase())
                                                    if (it.type == MessagePool.Type.Friend && it.extra != null) {
                                                        addProperty("sender", it.extra!!.getString("sender"))
                                                    }
                                                }
                                            )
                                        }
                                        Base.publicMsgPool.forEach {
                                            messages.add(
                                                JsonObject().apply {
                                                    addProperty("id", it.id)
                                                    addProperty("text", it.text)
                                                    if (find.name != null)
                                                        addProperty("read", it.extra?.contains(find.name!!) == true)
                                                    addProperty("type", "public")
                                                }
                                            )
                                        }
                                        add("messages", messages)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    addProperty("r", -1)
                                }
                            }
                        }.toString().toByteArray()
                    }
                    "change" -> {
                        val info = users[e.remoteAddress.address]
                        val which = url.queryParameter("which")

                        if (info == null || which == null) {
                            invalidFile.readBytes()
                        } else if (which == "face") {
                            val inputStream = e.requestBody
                            val outputStream = info.face.let {
                                if (!it.parentFile.exists()) it.parentFile.mkdirs()
                                val tmp = File(it.path + ".tmp")
                                if (!tmp.exists()) tmp.createNewFile()
                                tmp
                            }.outputStream()
                            var b = inputStream.read()
                            var count = 0
                            try {
                                while (b != -1) {
                                    count++
                                    outputStream.write(b)
                                    b = inputStream.read()
                                }
                            } finally {
                                outputStream.flush()
                                outputStream.close()
                                inputStream.close()
                            }
                            JsonObject().apply {
                                addProperty(
                                    "r",
                                    if (count >= 48) 0 else 1
                                )
                            }.toString().toByteArray()
                        } else {
                            JsonObject().apply {
                                when (which) {
                                    "nickname" -> {
                                        val what = e.requestBody.bufferedReader().readText()
                                        when {
                                            what.isEmpty() -> addProperty("r", 1)
                                            what.length > 20 -> addProperty("r", 2)
                                            else -> {
                                                info.nickname = what
                                                info.saveTag()
                                                addProperty("r", 0)
                                            }
                                        }
                                    }
                                    "empty" -> {
                                        val what = url.queryParameter("what")
                                        when (what) {
                                            "face" -> {
                                                val r = info.face.delete()
                                                addProperty("r", if (r) 0 else 1)
                                            }
                                            else -> addProperty("r", 503)
                                        }
                                    }
                                    "read" -> {
                                        val what = url.queryParameter("what")
                                        if (what != null) {
                                            val index = if (what.startsWith('p')) {
                                                what.removePrefix("p").toIntOrNull() to true
                                            } else {
                                                what.toIntOrNull() to false
                                            }
                                            if (index.first == null) {
                                                addProperty("r", -1)
                                            } else {
                                                val r = if (index.second) {
                                                    Base.publicMsgPool.markAsRead(index.first!!, info)
                                                } else {
                                                    info.messagePool.markAsRead(index.first!!)
                                                }
                                                addProperty("r", if (r) 0 else 1)
                                            }
                                        }
                                    }
                                    "unread" -> {
                                        val what = url.queryParameter("what")
                                        if (what != null) {
                                            val index = if (what.startsWith('p')) {
                                                what.removePrefix("p").toIntOrNull() to true
                                            } else {
                                                what.toIntOrNull() to false
                                            }
                                            if (index.first == null) {
                                                addProperty("r", 503)
                                            } else {
                                                val r = if (index.second) {
                                                    Base.publicMsgPool.markAsUnread(index.first!!, info)
                                                } else {
                                                    info.messagePool.markAsUnread(index.first!!)

                                                }
                                                addProperty("r", if (r) 0 else 1)
                                            }
                                        }
                                    }
                                    else -> addProperty("r", 503)
                                }
                            }.toString().toByteArray()
                        }
                    }
                    else -> invalidFile.readBytes()
                }
            }
            require == "env" -> {
                val get = url.queryParameter("get")
                if (get != null) {
                    env.get(get, "").toString().toByteArray()
                } else invalidFile.readBytes()
            }
            require == "lang" -> {
                val get = url.queryParameter("get")
                if (get != null) {
                    val find = users[e.remoteAddress.address]
                    if (find != null) {
                        Language[find.userLanguage, get]
                    } else {
                        Language.getDefault(get)
                    }.toByteArray()
                } else invalidFile.readBytes()
            }
            require == "ping" -> {
                val which = url.queryParameter("which")
                if (which == null)
                    "hello".toByteArray()
                else {
                    JsonObject().apply {
                        when (which) {
                            "dps" -> {
                                var stop = false
                                if (dpsed.containsKey(e.remoteAddress.address)) {
                                    if (System.currentTimeMillis() - dpsed[e.remoteAddress.address]!! <= 2000) {
                                        stop = true
                                    }
                                }
                                if (!stop) {
                                    dpsed[e.remoteAddress.address] = System.currentTimeMillis()
                                    val a = System.currentTimeMillis()
                                    var s = 0L
                                    while (true) {
                                        s++
                                        if (System.currentTimeMillis() - a >= 1000) {
                                            break
                                        }
                                    }
                                    addProperty("r", 0)
                                    addProperty("dps", s)
                                } else {
                                    addProperty("r", 504)
                                }
                            }
                            "player" -> {
                                addProperty("online", Bukkit.getOnlinePlayers().size)
                                addProperty("maxOnline", Bukkit.getMaxPlayers())
                                addProperty("totalPlayers", ServerPlayer.size)
                            }
                            else -> addProperty("r", 404)
                        }
                    }.toString().toByteArray()
                }
            }
            require == "header" -> File(root, "header.html").readBytes()
            else -> {
                if (require.isNotEmpty()) {
                    redirect("/", e)
                    return
                }
                File(root, "index.html").readBytes()
            }
        }
        response(response, e)
    }

    companion object {
        fun response(content: ByteArray, e: HttpExchange) {
            e.responseHeaders.add("Access-Control-Allow-Origin", "*")
            e.sendResponseHeaders(200, content.size.toLong())
            e.responseBody.apply {
                write(content)
                close()
            }
        }

        fun redirect(where: String, e: HttpExchange) {
            println("Redirecting ${e.remoteAddress.hostName} to $where")
            response("<script>window.location.href=\"$where\"</script>".toByteArray(), e)
        }
    }

    private fun createInfo(name: String): WebInfo? {
        val index = users.values.firstOrNull { it.name == name }
        return if (index != null) {
            index
        } else {
            val uuid = OfflineInfo.listPlayers { it.name == name }.firstOrNull()?.uuid
            if (uuid != null)
                RegisteredInfo(uuid)
            else {
                if (PreregisteredInfo.exist(name))
                    PreregisteredInfo(name)
                else null
            }
        }
    }

    private fun register(name: String, address: InetAddress): WebInfo {
        return PreregisteredInfo(name).also {
            users[address] = it
        }
    }

    private fun containsUser(address: InetAddress) = users.containsKey(address)
}