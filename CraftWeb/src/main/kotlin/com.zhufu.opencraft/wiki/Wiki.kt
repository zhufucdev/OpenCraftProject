package com.zhufu.opencraft.wiki

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.zhufu.opencraft.CraftWeb
import java.io.File
import java.net.InetAddress
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.fixedRateTimer

object Wiki {
    val wikiRoot = File(CraftWeb.plugin.config.getString("wikiRoot")!!)
    val wikiBackup = File(wikiRoot, "history").also { if (!it.exists()) it.mkdirs() }
    val backupTimeFormatter by lazy { SimpleDateFormat("@yyyy-MM-dd-HH:mm:ss") }
    private val lockers = HashMap<InetAddress, Pair<Long, String>>()

    fun backup(title: String) {
        val date = backupTimeFormatter.format(Date())
        val timePoint = Paths.get(wikiBackup.path, title, date).toFile()
        timePoint.mkdirs()

        val article = File(wikiRoot, "$title.html")
        if (!article.exists()) return
        article.copyTo(File(timePoint, "$title.html"))
        val infoWriter = File(timePoint, "$title.json").bufferedWriter()
        val editedInfo = getResourceConfig(title)
        editedInfo.addProperty("backup", date)
        GsonBuilder().setPrettyPrinting().create()
            .toJson(editedInfo, infoWriter)
        infoWriter.apply {
            flush()
            close()
        }
    }

    fun getResourceConfigFile(title: String, isImage: Boolean = false) =
        if (!isImage) File(wikiRoot, "$title.json") else Paths.get(wikiRoot.path, "images", "$title.json").toFile()!!

    fun getResourceConfig(title: String, isImage: Boolean = false) =
        getResourceConfigFile(title, isImage).let {
            JsonParser().parse(it.reader()).asJsonObject
        }

    fun lock(title: String, host: InetAddress): Boolean =
        isLocked(title, host).let {
            if (it == false) {
                lockers[host] = System.currentTimeMillis() to title
                val twoMinutes = 2 * 60 * 1000L
                fun delayTwoMinutes() {
                    fixedRateTimer(
                        initialDelay = twoMinutes,
                        period = 10,
                        action = {
                            // If the lock is still what the lock was
                            if (lockers[host]?.second == title) {
                                if (System.currentTimeMillis() - lockers[host]!!.first > twoMinutes) {
                                    unlock(title, host)
                                } else {
                                    delayTwoMinutes()
                                }
                            }
                            this.cancel()
                        }
                    )
                }
                delayTwoMinutes()
                true
            } else false
        }

    fun renewLock(title: String, host: InetAddress) =
        if (isLocked(title, host).also { print(it) } != true) {
            false
        } else {
            lockers[host] = System.currentTimeMillis() to title
            true
        }

    /**
     * @return if [host] == null true when file of [title] is locked, false otherwise.
     * else true if file of [title] is locked by [host], null if not, or false if the file isn't locked.
     */
    fun isLocked(title: String, host: InetAddress? = null): Boolean? {
        if (host == null)
            return lockers.any { it.value.second == title }
        lockers.forEach { (h, p) ->
            if (p.second == title) {
                return if (h == host)
                    true
                else
                    null
            }
        }
        return false
    }

    fun unlock(title: String, host: InetAddress): Boolean {
        if (lockers[host]?.second == title) {
            lockers.remove(host)
            return true
        }
        return false
    }

    fun updateResourceConfig(title: String, isImage: Boolean, block: JsonObject.() -> Unit) {
        val file = getResourceConfigFile(title, isImage)
        val config =
            if (!file.exists()) {
                if (!file.parentFile.exists()) file.parentFile.mkdirs()
                file.createNewFile()
                CraftWeb.logger.warning("Wiki info for $title doesn't exist. Generated automatically at ${file.path}.")
                JsonObject().also {
                    val writer = file.writer()
                    Gson().toJson(it, writer)
                    writer.apply {
                        flush()
                        close()
                    }
                }
            } else {
                JsonParser().parse(file.reader()).asJsonObject
            }
        block(config)
        file.writer().apply {
            GsonBuilder().setPrettyPrinting().create()
                .toJson(config, this)
            flush()
            close()
        }
    }

    fun forEachArticle(l: (JsonObject, String) -> Unit) {
        fun list(root: File) {
            root.listFiles()?.forEach {
                if (!it.isHidden) {
                    if (it.isFile) {
                        if (it.extension == "json")
                            try {
                                l(
                                    JsonParser().parse(it.bufferedReader()).asJsonObject,
                                    it.toRelativeString(wikiRoot).removeSuffix(".json")
                                )
                            } catch (e: Exception) {
                                CraftWeb.logger.warning("Failed to load wiki info for ${it.path} @ Wiki.forEach")
                                e.printStackTrace()
                            }
                    } else if (it.name != "images") {
                        list(it)
                    }
                }
            }
        }
        list(wikiRoot)
    }

    fun forEachImage(l: (JsonObject, String) -> Unit) {
        File(wikiRoot, "images").listFiles()?.forEach {
            if (it.isFile && it.extension == "json")
                try {
                    l(
                        JsonParser().parse(it.bufferedReader()).asJsonObject,
                        it.toRelativeString(wikiRoot).removeSuffix(".json")
                    )
                } catch (e: Exception) {
                    CraftWeb.logger.warning("Failed to load wiki info for ${it.path} @ Wiki.forEach")
                }
        }
    }

    fun forEach(l: (JsonObject, String) -> Unit) {
        forEachArticle(l)
        forEachImage(l)
    }
}


