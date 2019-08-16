package com.zhufu.opencraft.lobby

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.zhufu.opencraft.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.Executors

class PlayerLobby(val owner: OfflineInfo) {
    val id = owner.territoryID
    val x: Int
    val z: Int

    init {
        Base.getUniquePair(id).apply {
            x = first
            z = second
        }
    }

    /**
     * [fromX] is always less than [toX]
     * One's lobby is where he or she can build freely.
     */
    val fromX = 64 * x - 32
    val toX = fromX + 31
    val fromZ = 64 * z - 32
    val toZ = fromZ + 31
    val tagFile: File
        get() = Paths.get("plugins", "lobbies", "$id.yml").toFile().also {
            if (!it.parentFile.exists())
                it.parentFile.mkdirs()
            if (!it.exists())
                it.createNewFile()
        }
    val tag = YamlConfiguration.loadConfiguration(tagFile)
    val isInitialized get() = tag.getBoolean("initialized", false)
    var spawnPoint
        get() = tag.getObject("spawnpoint", Location::class.java, null)
        set(value) {
            tag.set("spawnpoint", value)
        }

    fun likeBy(who: ServerPlayer): Boolean {
        if (reviewedBy(who) == null) {
            tag.set("review.${who.name}", true)
            return true
        }
        return false
    }

    fun dislikeBy(who: ServerPlayer): Boolean {
        if (reviewedBy(who) == null) {
            tag.set("review.${who.name}", false)
            return true
        }
        return false
    }

    fun cancelReviewFor(who: ServerPlayer) = tag.set("review.${who.name}", null)
    /**
     * @return true when getting a like, false when getting a dislike, or null when getting nothing
     */
    fun reviewedBy(who: ServerPlayer) =
        if (tag.isSet("review.${who.name}")) tag.getBoolean("review.${who.name}") else null
    val likesInAll: Int
        get() {
            val review = tag.getConfigurationSection("review") ?: return 0
            var r = 0
            review.getKeys(false).forEach {
                if (review.getBoolean(it)) {
                    r++
                } else {
                    r--
                }
            }
            return r
        }

    fun contains(location: Location) = location.blockX in fromX..toX && location.blockZ in fromZ..toZ

    private var isInitializing = false
    fun initialize() {
        isInitializing = true
        var doneA = false
        var doneB = false

        var lowY = 2
        var highY = 256

        val barriers = arrayListOf<Int>()
        Bukkit.getScheduler().runTaskAsynchronously(Base.pluginCore) { _ ->
            val pool = Executors.newCachedThreadPool()
            run {
                fun newLocation(x: Int, y: Int, z: Int) =
                    Location(Base.lobby, x.toDouble() + fromX, y.toDouble(), z.toDouble() + fromZ)

                fun once(a: Int, b: Int) {
                    for (x in 0 until 32)
                        for (y in a until b)
                            for (z in 0 until 32) {
                                val copy = Base.spawnWorld.getBlockAt(x, y, z)
                                if (copy.type.name.contains("_BED")) {
                                    spawnPoint = newLocation(x, y, z)
                                } else if (copy.type == Material.BARRIER) {
                                    barriers.add(copy.location.blockY)
                                }
                            }
                }
                pool.execute {
                    once(10, 127)
                    doneA = true
                }
                pool.execute {
                    once(128, 256)
                    doneB = true
                }
            }
            while (!doneA || !doneB) {
                Thread.sleep(20)
            }
            pool.shutdown()

            barriers.min()?.let { lowY = it + 1 }
            barriers.max()?.let { highY = it - 1 }

            Bukkit.getScheduler().runTask(Base.pluginCore) { _ ->
                val from = CuboidRegion(
                    BukkitWorld(Base.spawnWorld),
                    BlockVector3.at(0, lowY, 0),
                    BlockVector3.at(32, highY, 32)
                )
                val to = CuboidRegion(
                    BukkitWorld(Base.lobby),
                    BlockVector3.at(this.fromX, lowY, this.fromZ),
                    BlockVector3.at(this.toX, highY, this.toZ)
                )
                val session = WorldEdit.getInstance().editSessionFactory.getEditSession(from.world, -1)
                Operations.complete(
                    ForwardExtentCopy(session, from, BukkitWorld(Base.lobby), to.minimumPoint).apply {
                        isCopyingBiomes = true
                        isCopyingEntities = true
                    }
                )
                tag.set("initialized", true)
                save()
                isInitializing = false
            }
        }
    }

    fun tpThere(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(Base.pluginCore) { _ ->
            while (isInitializing) {
                Thread.sleep(200)
            }
            Bukkit.getScheduler().runTask(Base.pluginCore) { _ ->
                if (spawnPoint != null) {
                    player.teleport(spawnPoint!!)
                } else {
                    player.warn(player.getter()["lobby.warn.noSpawnpoint", owner.name])
                    player.teleport(Location(Base.lobby, fromX.toDouble() + 16, 128.0, fromZ.toDouble() + 16))
                }
                PlayerLobbyManager.targetMap[player] = this
                val getter = player.getter()
                player.info()?.apply {
                    if (!isSurvivor && (player.info()?.territoryID ?: -1) == this@PlayerLobby.id) {
                        // To make speeches
                        var delay = 20L
                        fun speak(order: Int, arg: String? = null, block: (String) -> Unit) {
                            val string =
                                if (arg != null) getter["lobby.speeches.$order", arg] else getter["lobby.speeches.$order", arg]
                            var length = 0
                            string.forEach { if (it.isLetterOrDigit()) length++ }
                            delay += length
                            Bukkit.getScheduler().runTaskLater(Base.pluginCore, { _ ->
                                block(string)
                            }, delay)
                        }
                        speak(1) {
                            player.success(it)
                        }
                        speak(2, displayName) {
                            player.sendMessage(it)
                        }
                        speak(3) {
                            player.tip(it)
                        }
                    }
                }
            }
        }
    }

    fun save() {
        tag.save(tagFile)
    }
}