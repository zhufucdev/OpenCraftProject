package com.zhufu.opencraft

import com.mongodb.client.model.Filters
import com.zhufu.opencraft.api.Nameable
import com.zhufu.opencraft.data.Database
import com.zhufu.opencraft.data.ServerPlayer
import org.bson.Document
import org.bukkit.Bukkit
import java.io.File
import java.net.URL
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.math.roundToLong

/**
 * Represents a rented ad.
 */
open class Advertisement(
    val id: UUID = UUID.randomUUID(),
    var bonus: Long = 0,
    val owner: ServerPlayer,
    override var name: String = "",
    val startTime: Instant = Instant.now(),
    var lastCharge: Instant = Instant.now(),
    var duration: Duration,
    var size: Size
) : Nameable, Cloneable {
    enum class Duration(val ticks: Long) {
        FIVE_SEC(100), TEN_SEC(200), FIFTEEN_SEC(300);

        companion object {
            fun of(ticks: Long) = values().first { it.ticks == ticks }
        }
    }

    enum class Size(val priseScale: Double) {
        SMALL(1.0), ADAPTIVE(1.4), LARGE(1.8)
    }

    fun time() = Advertisement(
        id, bonus, owner, name, Instant.now(), Instant.now(), duration, size
    )

    /**
     * Sync to Database.
     */
    fun update() {
        val bson = toBson()
        val coll = Database.ads()
        val filter = Filters.eq("_id", id)
        if (coll.find(filter).first() == null) {
            coll.insertOne(bson)
        } else {
            coll.findOneAndReplace(filter, bson)
        }
    }

    /**
     * Remove from Database.
     */
    fun cancel() {
        Database.ads().deleteOne(Filters.eq("_id", id))
    }

    val image: File get() = Paths.get("plugins", "ads", "$id.png").toFile()

    enum class CacheResult {
        DENIED, SUCCESS, ERROR
    }

    private val cacheProtocols = listOf("http", "https")
    fun cacheImage(imageUrl: String): CacheResultContent {
        return try {
            val url = URL(imageUrl)
            if (url.protocol !in cacheProtocols) {
                return CacheResult.DENIED to null
            }
            val connection = url.openConnection()
            val image = ImageIO.read(connection.inputStream)
            ImageIO.write(image, "png", this.image)
            CacheResult.SUCCESS to null
        } catch (e: Exception) {
            Bukkit.getLogger().warning("Failed to cache ad image for ${owner.name}.")
            e.printStackTrace()
            CacheResult.ERROR to e
        }
    }

    val unitPrise: Long get() = (duration.ticks * 5 * size.priseScale).roundToLong() + bonus
    val weight get() = unitPrise * 0.6 + bonus * 0.4
    fun weigh(ads: List<Advertisement>): Double {
        val sum = ads.sumOf { it.weight }
        return weight / sum
    }

    private fun toBson() =
        Document("_id", id)
            .append("bonus", bonus)
            .append("owner", owner.uuid)
            .append("duration", duration.ticks)
            .append("startTime", startTime.epochSecond)
            .append("lastCharge", lastCharge.epochSecond)
            .append("size", size.name)
            .also {
                name.takeIf { it.isNotEmpty() }?.let { n -> it.append("name", n) }
            }

    public override fun clone(): Any {
        return Advertisement(id, bonus, owner, name, startTime, lastCharge, duration, size)
    }

    companion object {
        fun list(): Iterable<Advertisement> =
            Database.ads().find()
                .map {
                    Advertisement(
                        id = it.get("_id", UUID::class.java),
                        name = it.getString("name") ?: "",
                        bonus = it.getLong("bonus"),
                        owner = ServerPlayer.of(uuid = it.get("owner", UUID::class.java)),
                        duration = Duration.of(it.getLong("duration")),
                        startTime = Instant.ofEpochSecond(it.getLong("startTime")),
                        lastCharge = Instant.ofEpochSecond(it.getLong("lastCharge")),
                        size = Size.valueOf(it.getString("size"))
                    )
                }
    }
}

typealias CacheResultContent = Pair<Advertisement.CacheResult, Exception?>