package com.zhufu.opencraft

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.Bukkit
import org.bukkit.Server
import java.nio.file.Paths

object ServerStatics {
    private val save get() = Paths.get("plugins", "statics", "server.json").toFile()
    private lateinit var data: JsonObject
    var playerNumber: Int
        get() = data["players"]?.asInt ?: 0
        set(value) {
            data.addProperty("players", value)
        }
    var onlineTime: Long
        get() = data["online"]?.asLong ?: 0
        set(value) {
            data.addProperty("online", value)
        }

    fun init() {
        data = if (!save.exists()) {
            if (!save.parentFile.exists()) {
                save.parentFile.mkdirs()
            }
            save.createNewFile()
            JsonObject()
        } else {
            try {
                JsonParser.parseReader(save.reader()).asJsonObject
            }catch (e: Exception){
                Bukkit.getLogger().warning("Server statics save could not be loaded. Use an empty file.")
                JsonObject()
            }
        }
    }

    fun save() {
        val writer = save.writer()
        GsonBuilder()
            .setPrettyPrinting()
            .create()
            .toJson(data, writer)
        writer.apply {
            flush()
            close()
        }
    }
}