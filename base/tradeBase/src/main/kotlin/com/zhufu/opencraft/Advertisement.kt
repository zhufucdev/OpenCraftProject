package com.zhufu.opencraft

import com.zhufu.opencraft.api.Nameable
import com.zhufu.opencraft.data.ServerPlayer
import org.bukkit.Bukkit
import java.io.File
import java.net.URL
import java.nio.file.Paths
import java.time.Instant
import java.util.*
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
    var enabled: Boolean = true,
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
        id, bonus, owner, name, enabled, Instant.now(), Instant.now(), duration, size
    )

    /**
     * Sync to Database.
     */
    fun update() {
        syncImpl.update(this)
    }

    /**
     * Remove from Database.
     */
    fun cancel() {
        syncImpl.cancel(this)
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
            this.image.parentFile.takeUnless { it.exists() }?.mkdirs()
            ImageIO.write(image, "png", this.image)
            CacheResult.SUCCESS to null
        } catch (e: Exception) {
            Bukkit.getLogger().warning("Failed to cache ad image for ${owner.name}.")
            e.printStackTrace()
            CacheResult.ERROR to e
        }
    }

    val unitPrise: Long
        get() =
            if (enabled)
                (duration.ticks * 5 * size.priseScale).roundToLong() + bonus
            else
                0
    val weight get() = unitPrise * 0.6 + bonus * 0.4
    fun weigh(ads: Iterable<Advertisement>): Double {
        val sum = ads.sumOf { it.weight }
        return weight / sum
    }

    public override fun clone(): Any {
        return Advertisement(id, bonus, owner, name, enabled, startTime, lastCharge, duration, size)
    }

    companion object {
        private lateinit var syncImpl: AdSync

        fun setImpl(impl: AdSync) {
            this.syncImpl = impl
        }

        fun list(): Iterable<Advertisement> = syncImpl.list()
    }
}

typealias CacheResultContent = Pair<Advertisement.CacheResult, Exception?>

interface AdSync {
    fun update(ad: Advertisement)
    fun cancel(ad: Advertisement)
    fun list(): Iterable<Advertisement>
}