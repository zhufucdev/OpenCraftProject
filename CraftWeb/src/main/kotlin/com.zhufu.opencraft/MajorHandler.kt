package com.zhufu.opencraft

import com.google.gson.*
import com.sun.net.httpserver.HttpExchange
import com.zhufu.opencraft.CraftWeb.Companion.logger
import com.zhufu.opencraft.Game.env
import com.zhufu.opencraft.WebInfo.Companion.users
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.wiki.Search
import com.zhufu.opencraft.wiki.Wiki
import okhttp3.HttpUrl
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import java.io.File
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Paths
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MajorHandler(private val root: File, private val wikiRoot: File, private val conf: FileConfiguration) :
    MyHandler(logger) {
    private val invalidFile: File
        get() = File(root, "invalidRequest.html")
    private val noSuchContentFile: File
        get() = File(root, "404.html")
    private val dpsed = HashMap<InetAddress, Long>()

    override fun handleWithExceptions(exchange: HttpExchange) {
        val require = exchange.requestURI.path.removePrefix("/")

        val url = HttpUrl.get("https://" + conf.getString("hostName") + exchange.requestURI)
        if (url == null) {
            response(invalidFile.readBytes(), exchange, "text/html")
            return
        }
        val remoteAddress: InetAddress = url.queryParameter("hostName").let {
            if (it == null) {
                exchange.remoteAddress.address.let { address ->
                    if (address.hostName == "localhost")
                        InetAddress.getByName("127.0.0.1")!!
                    else
                        address
                }
            } else {
                InetAddress.getByName(it)!!
            }
        }
        val response: Pair<ByteArray, MimeType> = when {
            require.contains('/') && !require.contains("navigate") && !require.contains("wiki") -> {
                val file = File(root, require).let {
                    if (it.exists())
                        it
                    else
                        noSuchContentFile
                }
                response(file.inputStream(), file.size(), exchange, file.mimeType.toString())
                return
            }
            require == "ui" && url.queryParameter("request") != null -> {
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
                } to MimeType.HTML
            }
            require == "user" -> {
                when (url.queryParameter("request")) {
                    "login" -> {
                        val obj = JsonObject()
                        val json = JsonParser().parse(exchange.requestBody.bufferedReader()).asJsonObject
                        if (json == null) {
                            obj.addProperty("r", -2)
                        } else {
                            val name = json["name"]?.asString
                            val pwd = json["password"]?.asString
                            if (name == null || pwd == null) {
                                response(invalidFile.readBytes(), exchange, "text/html")
                                return
                            }

                            val info = createInfo(name)
                            obj.apply {
                                when {
                                    containsUser(remoteAddress) ->
                                        addProperty("r", 2)
                                    info == null -> //When no such user
                                        addProperty("r", -1)
                                    info.password != pwd -> //When wrong password
                                        addProperty("r", 1)
                                    else -> {
                                        //When success
                                        addProperty("r", 0)
                                        users.forEach { (t, u) ->
                                            if (u == info)
                                                users.remove(t)
                                        }

                                        users[remoteAddress] = info
                                        logger.info("$remoteAddress has log in.")
                                    }
                                }
                            }
                        }
                        obj.toString().toByteArray() to MimeType.JSON
                    }
                    "logout" -> {
                        val obj = JsonObject()
                        obj.apply {
                            if (containsUser(remoteAddress)) {
                                users[remoteAddress]!!.saveTag()
                                val removed = users.remove(remoteAddress)!!
                                removed.lastExchange?.apply {
                                    try {
                                        response(
                                            "\$json:${JsonObject().apply { addProperty("r", 503) }}".toByteArray(),
                                            this
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                PlayerManager.remove(removed)
                                addProperty("r", 0)
                                logger.info("$remoteAddress has log out.")
                            } else {
                                addProperty("r", -1)
                            }
                        }.toString().toByteArray() to MimeType.JSON
                    }
                    "sign-up" -> {
                        val obj = JsonObject()
                        obj.apply {
                            if (containsUser(remoteAddress)) {
                                addProperty("r", 2)
                            } else {
                                val json = JsonParser().parse(exchange.requestBody.bufferedReader()).asJsonObject
                                if (json == null)
                                    addProperty("r", -2)
                                else {
                                    val id = json["name"]?.asString
                                    val pwd = json["password"]?.asString
                                    val nick = json["nickname"]?.asString
                                    if (id == null || pwd == null) {
                                        addProperty("r", -1)
                                    } else if (PreregisteredInfo.exists(id) || OfflineInfo.listPlayers { it.name == id }.isNotEmpty()) {
                                        addProperty("r", 2)
                                    } else {
                                        val register = register(id, remoteAddress)
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

                        obj.toString().toByteArray() to MimeType.JSON
                    }
                    "destroy" -> {
                        val find = users[remoteAddress]
                        JsonObject().apply {
                            if (find == null) {
                                addProperty("r", -1)
                            } else {
                                find.tagFile.deleteRecursively()
                                users.remove(remoteAddress)
                                ServerStatics.playerNumber--
                                addProperty("r", 0)
                            }
                        }.toString().toByteArray() to MimeType.JSON
                    }

                    "check" -> {
                        val obj = JsonObject()
                        val find = users[remoteAddress]
                        obj.apply {
                            if (find == null) {
                                /** r = -1 **/
                                addProperty("r", -1)
                            } else {
                                try {
                                    val check = ArrayList<String>()
                                    check.addAll(url.queryParameter("check")?.split(',') ?: listOf("all"))
                                    if (check.contains("face")) {
                                        if (find.face.exists()) {
                                            response(find.face.readBytes(), exchange)
                                        } else {
                                            response(ByteArray(0), exchange)
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
                                                /** r = 0 **/
                                                0
                                            } else -1)
                                            response(obj.toString().toByteArray(), exchange)
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
                        }.toString().toByteArray() to MimeType.JSON
                    }
                    "change" -> {
                        val info = users[remoteAddress]
                        val which = url.queryParameter("which")

                        if (info == null || which == null) {
                            invalidFile.readBytes() to MimeType.HTML
                        } else if (which == "face") {
                            val inputStream = exchange.requestBody
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
                            }.toString().toByteArray() to MimeType.JSON
                        } else {
                            JsonObject().apply {
                                when (which) {
                                    "nickname" -> {
                                        val what = exchange.requestBody.bufferedReader().readText()
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
                                        when (url.queryParameter("what")) {
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
                            }.toString().toByteArray() to MimeType.JSON
                        }
                    }

                    "chat" -> {
                        JsonObject().apply {
                            val message = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                            val find = users[remoteAddress]
                            if (find == null) {
                                addProperty("r", -1)
                            } else {
                                Translator.chat(message, find)
                                addProperty("r", 0)
                            }
                        }.toString().toByteArray() to MimeType.JSON
                    }

                    "dir" -> JsonObject().apply {
                        val find = users[remoteAddress]
                        if (find == null) {
                            addProperty("r", 503)
                        } else {
                            val path = url.queryParameter("path")
                            val operation = url.queryParameter("operation") ?: "check"

                            if (path == null || !path.contains("..")) {
                                fun serialize(it: File) = JsonObject().apply {
                                    addProperty("name", it.name)
                                    addProperty("directory", it.isDirectory)
                                    if (it.isDirectory) {
                                        if (find.playerDir == it) addProperty("isRoot", true)
                                        addProperty("children", it.listFiles()?.size ?: 0)
                                    }
                                    addProperty("size", it.size())
                                }

                                if (operation == "del") {
                                    val json = JsonParser().parse(exchange.requestBody.bufferedReader())
                                    val failure = JsonArray()
                                    json.asJsonArray.forEach {
                                        val file = File(find.playerDir, it.asString)
                                        file.deleteRecursively().let { success ->
                                            if (!success) failure.add(file.name)
                                        }
                                    }
                                    addProperty("r", if (failure.size() == 0) 0 else 1)
                                    if (failure.size() > 0)
                                        add("failure", failure)
                                } else {
                                    val file = File(find.playerDir, path ?: "")
                                    when (operation) {
                                        "check" -> {
                                            if (!file.exists()) {
                                                addProperty("r", 1)
                                            } else if (file.isDirectory) {
                                                val files = JsonArray()
                                                file.listFiles()?.forEach {
                                                    files.add(serialize(it))
                                                }
                                                add("r", files)
                                            } else {
                                                add("r", serialize(file))
                                            }
                                        }
                                        "read" -> {
                                            if (file.isFile) {
                                                response(file.readBytes(), exchange)
                                                return
                                            } else {
                                                addProperty("r", 1)
                                            }
                                        }
                                        "write" -> {
                                            /**
                                             * [response].r == 0 when success
                                             *              == 1 when request is invalid
                                             *              == 2 when out-of-spaced
                                             *              ==-1 when catching an exception
                                             */
                                            if (file.isFile || !file.exists()) {
                                                if (!file.exists()) file.createNewFile()

                                                val output = file.outputStream()
                                                var result = 0
                                                val input = exchange.requestBody
                                                try {
                                                    var size = 0
                                                    val freeSpace =
                                                        conf.getInt("playerDirMaxSize") - find.playerDir.size()
                                                    var r = input.read()
                                                    while (r >= 0) {
                                                        if (size > freeSpace) {
                                                            result = 2
                                                            break
                                                        }

                                                        output.write(r)
                                                        size++
                                                        r = input.read()
                                                    }
                                                } catch (e: Exception) {
                                                    result = -1
                                                    logger.warning("Failed to write playerDir file at ${file.path} for player ${find.name}.")
                                                    e.printStackTrace()
                                                } finally {
                                                    output.flush()
                                                    output.channel.lock().release()
                                                    output.close()
                                                    input.close()
                                                }
                                                addProperty("r", result)
                                            } else {
                                                addProperty("r", 1)
                                            }
                                        }
                                        "rename" -> {
                                            if (!file.exists()) {
                                                addProperty("r", 1)
                                            } else {
                                                val newName = url.queryParameter("name")
                                                if (newName != null) {
                                                    if (newName.contains(".."))
                                                        addProperty("r", 503)
                                                    else {
                                                        file.renameTo(File(find.playerDir, newName))
                                                        addProperty("r", 0)
                                                    }
                                                } else
                                                    addProperty("r", 1)
                                            }
                                        }
                                        "mkdir" -> {
                                            if (file.isDirectory) {
                                                addProperty("r", 1)
                                            } else {
                                                addProperty("r", file.mkdirs().let {
                                                    if (it) 0
                                                    else -1
                                                })
                                            }
                                        }
                                    }
                                }
                            } else {
                                addProperty("r", 503)
                            }
                        }
                    }.toString().toByteArray() to MimeType.JSON
                    else -> invalidFile.readBytes() to MimeType.HTML
                }
            }
            require == "env" -> {
                val get = url.queryParameter("get")
                if (get != null) {
                    env.get(get, "").toString().toByteArray() to MimeType.PlainText
                } else invalidFile.readBytes() to MimeType.HTML
            }
            require == "lang" -> {
                val get = url.queryParameter("get")
                if (get != null) {
                    val find = users[remoteAddress]
                    if (find != null) {
                        Language[find.userLanguage, get]
                    } else {
                        Language.getDefault(get)
                    }.toByteArray() to MimeType.PlainText
                } else invalidFile.readBytes() to MimeType.HTML
            }
            require == "license" -> {
                File(root, "LICENSE").let {
                    if (it.exists()) it.readBytes()
                    else "Failed to fetch content.".toByteArray()
                } to MimeType.PlainText
            }
            require == "ping" -> {
                val which = url.queryParameter("which")
                if (which == null)
                    "hello".toByteArray() to MimeType.PlainText
                else {
                    JsonObject().apply {
                        when (which) {
                            "dps" -> {
                                var stop = false
                                if (dpsed.containsKey(remoteAddress)) {
                                    if (System.currentTimeMillis() - dpsed[remoteAddress]!! <= 2000) {
                                        stop = true
                                    }
                                }
                                if (!stop) {
                                    dpsed[remoteAddress] = System.currentTimeMillis()
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
                                addProperty("totalPlayers", ServerStatics.playerNumber)
                            }
                            else -> addProperty("r", 404)
                        }
                    }.toString().toByteArray() to MimeType.JSON
                }
            }
            require == "header" -> File(root, "header.html").readBytes() to MimeType.HTML
            require == "chat" -> {
                val find = users[remoteAddress]
                if (find == null) {
                    "\$json:${JsonObject().apply { addProperty("r", 503) }}".toByteArray() to MimeType.PlainText
                } else {
                    with(find) {
                        lastExchange = exchange
                        exchanges++
                    }
                    return
                }
            }
            require == "wiki" -> {
                val operation = url.queryParameter("operation") ?: "read"
                val title = url.queryParameter("title") ?: "frontPage"
                if (title.contains("..")) {
                    JsonObject().apply { addProperty("r", 503) }.toString().toByteArray() to MimeType.JSON
                } else {
                    when (operation) {
                        "read" -> {
                            val article = File(wikiRoot, "$title.html")
                            if (article.exists()) {
                                val info = File(wikiRoot, "$title.json").let {
                                    if (it.exists())
                                        JsonParser().parse(it.reader())
                                    else {
                                        logger.warning("Wiki info for $title doesn't exist. Generated automatically at ${it.path}.")
                                        if (!it.parentFile.exists())
                                            it.parentFile.mkdirs()
                                        it.createNewFile()
                                        JsonObject().apply {
                                            addProperty("title", title)
                                            addProperty("UUID", UUID.randomUUID().toString())

                                            val writer = it.writer()
                                            GsonBuilder().setPrettyPrinting().create().toJson(this, writer)
                                            writer.apply {
                                                flush()
                                                close()
                                            }
                                        }
                                    }
                                }
                                if (url.queryParameter("withScript") == "true") {
                                    "<script>${File(wikiRoot, "script.js").readText().replace(
                                        "\$title",
                                        title
                                    )}</script>".toByteArray() to MimeType.HTML
                                } else {
                                    JsonObject().apply {
                                        addProperty("content", article.readText())
                                        add("info", info)
                                    }.toString().toByteArray() to MimeType.JSON
                                }
                            } else {
                                JsonObject().apply {
                                    addProperty("r", 404)
                                }.toString().toByteArray() to MimeType.JSON
                            }
                        }
                        "write" -> {
                            JsonObject().apply {
                                when {
                                    !users.containsKey(remoteAddress) -> {
                                        addProperty("r", 503)
                                    }
                                    url.queryParameter("img") == "true" -> {
                                        val knownMD5 = url.queryParameter("md5")
                                        if (knownMD5 != null) {
                                            val outFile = Paths.get(wikiRoot.path, "images", title).toFile()
                                            if (outFile.exists() && outFile.MD5() == knownMD5) {
                                                addProperty("r", 0)
                                            } else {
                                                addProperty("r", 2)
                                            }
                                        } else {
                                            val input = exchange.requestBody
                                            var toValidate = false
                                            val outFile = Paths.get(wikiRoot.path, "images", title).toFile().let {
                                                if (!it.parentFile.exists())
                                                    it.parentFile.mkdirs()
                                                if (!it.exists()) {
                                                    it.createNewFile()
                                                    it
                                                } else {
                                                    addProperty("r", 2)
                                                    toValidate = true
                                                    createTempFile(it.nameWithoutExtension)
                                                }
                                            }

                                            val out = outFile.outputStream()
                                            var read = input.read()
                                            var count = 0
                                            while (read >= 0) {
                                                out.write(read)
                                                count++
                                                if (count > 20 * 1024 * 1024) {
                                                    addProperty("r", 1)
                                                    out.close()
                                                    outFile.delete()

                                                    input.close()
                                                    return@apply
                                                }
                                                read = input.read()
                                            }
                                            input.close()
                                            out.apply {
                                                flush()
                                                close()
                                            }

                                            if (!toValidate) addProperty("r", 0)
                                            else {
                                                val origin = Paths.get(wikiRoot.path, "images", title).toFile()
                                                if (origin.MD5() == outFile.MD5()) {
                                                    outFile.delete()
                                                    addProperty("r", 0)
                                                }
                                            }

                                            if (this["r"].asInt == 0) {
                                                Wiki.updateResourceConfig(title, true) {
                                                    addProperty("create", System.currentTimeMillis())
                                                    addProperty("isImage", true)
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        val article = File(wikiRoot, "$title.html").also {
                                            if (!it.parentFile.exists()) it.parentFile.mkdirs()
                                        }
                                        if (article.exists()) {
                                            val lock = Wiki.isLocked(title, remoteAddress)
                                            if (lock != true) {
                                                addProperty("r", if (lock == null) 10 else 11)
                                                return@apply
                                            }
                                        }
                                        Wiki.backup(title)
                                        // Read
                                        val input = try {
                                            JsonParser().parse(exchange.requestBody.bufferedReader()).asJsonObject
                                        } catch (e: Exception) {
                                            addProperty("r", -1)
                                            return@apply
                                        }
                                        if (!input.has("info") || !input["info"].isJsonObject
                                            || !input.has("content")
                                        ) {
                                            addProperty("r", -1)
                                            return@apply
                                        }
                                        val infoInput = input["info"].asJsonObject
                                        if (!infoInput.has("title")) {
                                            addProperty("r", -1)
                                            return@apply
                                        }

                                        if (!article.exists()) article.createNewFile()
                                        val infoFile = Wiki.getResourceConfigFile(title)

                                        class IgnoredException : Exception()
                                        try {
                                            Wiki.updateResourceConfig(title, false) {
                                                if (this.has("backup")) {
                                                    addProperty("r", 4)
                                                    throw IgnoredException()
                                                }

                                                val editorName: String
                                                if (infoInput.has("editor")) {
                                                    val authors = JsonArray()
                                                    editorName = infoInput["editor"].asString
                                                    if (editorName.length > 20) {
                                                        addProperty("r", 2)
                                                        throw IgnoredException()
                                                    } else {
                                                        // Append an author
                                                        var isAdded = false
                                                        if (this.has("author")) {
                                                            val author = this["author"]
                                                            if (author.isJsonArray) {
                                                                author.asJsonArray.forEach {
                                                                    authors.add(it.asString.also { oldName ->
                                                                        if (oldName == editorName)
                                                                            isAdded = true
                                                                    })
                                                                }
                                                            } else {
                                                                if (author.asString == editorName)
                                                                    authors.add(author.asString)
                                                                else
                                                                    isAdded = true
                                                            }
                                                        }
                                                        if (!isAdded)
                                                            authors.add(editorName)

                                                        this.add("author", authors)
                                                    }
                                                } else {
                                                    editorName = "anonymity"
                                                }
                                                // Sync subtitles
                                                if (infoInput.has("subtitle"))
                                                    addProperty("subtitle", infoInput["subtitle"].asString)
                                                else
                                                    remove("subtitle")
                                                // Sync tags
                                                if (infoInput.has("tag"))
                                                    add("tag", infoInput["tag"])
                                                else
                                                    remove("tag")

                                                addProperty("title", infoInput["title"].asString)
                                                // Update last change info
                                                add("lastChange", JsonObject().apply {
                                                    addProperty("time", System.currentTimeMillis())
                                                    addProperty("editor", editorName)
                                                })
                                                addProperty("isImage", false)
                                            }
                                            article.writeText(input["content"].asString)

                                            // If editor wants to rename
                                            if (infoInput.has("path")) {
                                                val newPath = infoInput["path"].asString
                                                val newArticle = File(wikiRoot, "$newPath.html")
                                                if (newArticle.exists()) {
                                                    addProperty("r", 3)
                                                } else {
                                                    val a = article.renameTo(newArticle)
                                                    val b = infoFile.renameTo(File(wikiRoot, "$newPath.json"))
                                                    if (!a || !b)
                                                        addProperty("r", 1)
                                                    else
                                                        addProperty("r", 0)
                                                }
                                            } else
                                                addProperty("r", 0)

                                            if (this["r"].asInt == 0) {
                                                Wiki.unlock(title, remoteAddress)
                                                broadcast(
                                                    "wiki.thanks",
                                                    TextUtil.TextColor.YELLOW,
                                                    users[remoteAddress]?.id,
                                                    infoInput["title"].asString
                                                )
                                            }
                                        } catch (ignored: IgnoredException) {

                                        }

                                    }
                                }
                            }.toString().toByteArray() to MimeType.JSON
                        }
                        "rename" -> {
                            JsonObject().apply {
                                if (!users.containsKey(remoteAddress)) {
                                    addProperty("r", 503)
                                } else {
                                    val dest = url.queryParameter("to")
                                    if (dest == null) {
                                        addProperty("r", -1)
                                    } else {
                                        val isImage = url.queryParameter("img") == "true"
                                        fun get(title: String) = if (isImage) Paths.get(
                                            wikiRoot.path,
                                            "images",
                                            title
                                        ).toFile() else File(wikiRoot, title)

                                        val tag = url.queryParameter("tag")
                                        if (tag != null) {
                                            val tags = JsonArray().apply {
                                                tag.split(',').forEach {
                                                    add(it)
                                                }
                                            }
                                            Wiki.updateResourceConfig(title, isImage) {
                                                if (tags.size() > 0)
                                                    add("tag", tags)
                                                else
                                                    remove("tag")
                                            }
                                        }
                                        if (title != dest) {
                                            val target = get(title)
                                            val targetInfo = Wiki.getResourceConfigFile(title, isImage)
                                            if (!target.exists()) {
                                                addProperty("r", 1)
                                            } else {
                                                val destFile = get(dest)
                                                val destInfoFile = Wiki.getResourceConfigFile(dest, isImage)
                                                if (destFile.exists())
                                                    addProperty("r", 2)
                                                else
                                                    addProperty("r", if (target.renameTo(destFile)) 0 else 3)

                                                if (!destInfoFile.exists())
                                                    targetInfo.renameTo(destInfoFile)
                                            }
                                        } else {
                                            addProperty("r", 0)
                                        }
                                    }
                                }
                            }.toString().toByteArray() to MimeType.JSON
                        }
                        "search" -> {
                            JsonObject().apply {
                                if (url.queryParameter("advanced") != "true") {
                                    val keywords = url.queryParameter("key")?.split(' ')
                                    if (keywords == null)
                                        addProperty("r", -1)
                                    else {
                                        val tag = url.queryParameter("tag")?.split(',') ?: emptyList()
                                        val type = url.queryParameter("type") ?: "any"
                                        // Search Begin
                                        val timeBegin = System.currentTimeMillis()
                                        val results = Search().apply {
                                            addCondition(Search.Condition(keywords, tag, type))
                                            doSearch()
                                        }
                                            .results.sortedWith(Comparator { o1, o2 -> ((o2.confidence - o1.confidence) * 10).toInt() })
                                        add("r", JsonArray().apply {
                                            results.forEach {
                                                if (it.confidence < 0.3F) return@forEach

                                                val keywordsMatch = JsonArray().apply {
                                                    it.keywords.forEach { keyword ->
                                                        add(keyword)
                                                    }
                                                }
                                                add(
                                                    JsonObject().apply {
                                                        addProperty("title", it.title)
                                                        addProperty("confidence", it.confidence)
                                                        add("info", it.info)
                                                        add("keywords", keywordsMatch)
                                                    }
                                                )
                                            }
                                        })
                                        addProperty("time", System.currentTimeMillis() - timeBegin)
                                    }
                                } else {
                                    TODO()
                                }
                            }.toString().toByteArray() to MimeType.JSON
                        }
                        "lock" -> {
                            JsonObject().apply {
                                if (!Wiki.getResourceConfig(title).has("backup"))
                                    addProperty("r", if (Wiki.lock(title, remoteAddress)) 0 else 1)
                                else
                                    addProperty("r", 2)
                            }.toString().toByteArray() to MimeType.JSON
                        }
                        "renewLock" -> {
                            JsonObject().apply {
                                addProperty("r", if (Wiki.renewLock(title, remoteAddress)) 0 else 1)
                            }.toString().toByteArray() to MimeType.JSON
                        }
                        "unlock" -> {
                            JsonObject().apply {
                                addProperty("r", if (Wiki.unlock(title, remoteAddress)) 0 else 1)
                            }.toString().toByteArray() to MimeType.JSON
                        }
                        "list" -> {
                            JsonArray().apply {
                                when (url.queryParameter("what")) {
                                    "class" -> {
                                        Wiki.forEachArticle { json, _ ->
                                            if (json.has("title")) {
                                                val classes = json["title"].asString.split('/')
                                                if (classes.size > 1) {
                                                    classes.forEach { clazz ->
                                                        if (!any { it.asString == clazz })
                                                            add(clazz)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    "article" -> {
                                        Wiki.forEachArticle { _, title ->
                                            add(title)
                                        }
                                    }
                                }
                            }.toString().toByteArray() to MimeType.JSON
                        }
                        else -> {
                            JsonObject().apply {
                                addProperty("r", -1)
                            }.toString().toByteArray() to MimeType.JSON
                        }
                    }
                }
            }
            require.startsWith("wiki/images") -> {
                val index = require.indexOf('/', 9)
                val noSuchImage = File(wikiRoot, "no-such-image.png")
                if (index == -1) {
                    noSuchImage.readBytes()
                } else {
                    val title = require.substring(index + 1)
                    val file = Paths.get(wikiRoot.path, "images", title).toFile()
                    file.let { if (it.exists()) it else noSuchImage }.readBytes()
                } to MimeType.PNG
            }
            require == "favicon.ico" -> File("favicon.ico").let {
                if (it.exists())
                    it to MimeType.ICO
                else
                    noSuchContentFile to MimeType.HTML
            }.let {
                it.first.readBytes() to it.second
            }
            else -> {
                if (require.isNotEmpty()) {
                    logger.info(require)
                    if (require.startsWith("navigate")) {
                        val navigation = exchange.requestURI
                        if (navigation.path.indexOf('/', 1) == -1) {
                            invalidFile.readBytes()
                        } else {
                            var invalid = false
                            val paths = navigation.path.split('/').drop(2)
                            val navigator: String? = when {
                                paths.first() == "wiki" -> {
                                    if (paths.size > 1)
                                        "wiki?title=${paths.drop(1).joinToString(separator = "/")}&withScript=true"
                                    else {
                                        "wiki?withScript=true"
                                    }
                                }
                                else -> "ui?request=${paths.first()}"
                            }
                            if (!invalid) {
                                logger.info("navigator is $navigator")
                                File(root, "navigator.html").readText().replace("\$navigation", navigator!!)
                                    .toByteArray()
                            } else {
                                invalidFile.readBytes()
                            }
                        }
                    } else {
                        logger.warning("$require is not allowed.")
                        redirect("/", exchange)
                        return
                    }
                } else {
                    File(root, "navigator.html").readText().replace("\$navigation", "ui?request=main").toByteArray()
                } to MimeType.HTML
            }
        }
        response(response.first, exchange, response.second.toString())
    }

    companion object {
        fun response(content: ByteArray, e: HttpExchange, type: String = "text/plain") {
            e.responseHeaders.apply {
                add("Access-Control-Allow-Origin", "*")
                set("Content-Type", type)
            }
            e.sendResponseHeaders(206, content.size.toLong())
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