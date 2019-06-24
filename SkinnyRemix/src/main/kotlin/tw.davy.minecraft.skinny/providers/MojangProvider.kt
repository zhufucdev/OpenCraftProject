package tw.davy.minecraft.skinny.providers

import com.google.gson.*
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.concurrent.TimeUnit

import tw.davy.minecraft.skinny.SignedSkin
import tw.davy.minecraft.skinny.Skinny

/**
 * @author Davy
 */
class MojangProvider : LegacyProvider() {

    protected override val skinFolder: File
        get() = File(Skinny.instance.dataFolder, "mojang_caches")

    override fun getSkinData(name: String): SignedSkin? {
        val uuid = getUUID(name) ?: return null

        val cachedSkin = super.getSkinData(uuid)
        val player = getPlayer(name)
        if (cachedSkin != null && player != null && player.lastPlayed < getCacheTime(uuid))
            return cachedSkin

        val data = readUrl("https://sessionserver.mojang.com/session/minecraft/profile/$uuid?unsigned=false")

        if (data.isEmpty() || data.contains("\"error\""))
            return cachedSkin

        try {
            val jsonData = JsonParser().parse(data) as JsonObject
            val properties = jsonData.get("properties").asJsonArray
            for (property in properties) {
                val prop = property.asJsonObject
                if (prop.get("name").asString != "textures")
                    continue

                val skin = SignedSkin(prop.get("value").asString, prop.get("signature").asString)
                createCache(uuid, skin)

                return skin
            }
        } catch (ignored: JsonParseException) {
        }

        return cachedSkin
    }

    private fun createCache(uuid: String, skin: SignedSkin) {
        if (!getSkinDir(uuid).exists())
            getSkinDir(uuid).mkdir()
        writeData(File(getSkinDir(uuid), "value.dat"), skin.value)
        writeData(File(getSkinDir(uuid), "signature.dat"), skin.signature)
        writeData(File(getSkinDir(uuid), "timestamp"), Instant.now().toEpochMilli().toString())
    }

    private fun getPlayer(name: String): OfflinePlayer? {
        for (player in Bukkit.getOfflinePlayers()) {
            if (player.name.equals(name, ignoreCase = true))
                return player
        }

        return null
    }

    private fun writeData(file: File, data: String) {
        try {
            val buf = BufferedWriter(FileWriter(file))
            buf.write(data)
            buf.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun getCacheTime(name: String): Long {
        val data = readData(File(getSkinDir(name), "timestamp"))
        try {
            return java.lang.Long.parseLong(data!!) + TimeUnit.MINUTES.toMillis(30)
        } catch (e: NumberFormatException) {
            return 0
        }

    }

    private fun getUUID(name: String): String? {
        val data = readUrl("https://api.mojang.com/users/profiles/minecraft/$name")
        val json = try {
            JsonParser().parse(data).asJsonObject
        } catch (e: Exception) {
            null
        }

        if (json == null || data.contains("\"error\""))
            return null
        return json["id"].asString
    }

    private fun readUrl(url: String): String {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Minecraft")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doOutput = true

            val output = StringBuilder()
            val buf = BufferedReader(InputStreamReader(connection.inputStream))

            var line = buf.readLine()
            while (line != null) {
                output.append(line)
                line = buf.readLine()
            }

            buf.close()

            return output.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return ""
    }
}
