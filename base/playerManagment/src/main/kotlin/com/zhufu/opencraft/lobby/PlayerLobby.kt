package com.zhufu.opencraft.lobby

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.zhufu.opencraft.*
import com.zhufu.opencraft.data.OfflineInfo
import com.zhufu.opencraft.data.ServerPlayer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.io.File
import java.nio.file.Paths
import kotlin.math.abs

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
     * [fromX] is always smaller than [toX]
     * One's lobby is where he or she can build freely.
     */
    val fromX = width * (x - 1)
    val toX = fromX + width - 1
    val fromZ = length * (z - 1)
    val toZ = fromZ + length - 1
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
        get() = tag.getLocation("spawnpoint", null)
        set(value) {
            tag.set("spawnpoint", value)
        }
    var views: Long
        get() = tag.getLong("views", 0L)
        set(value) = tag.set("views", value)
    var visitorGameMode: GameMode
        get() = GameMode.valueOf(tag.getString("visitorGameMode", "CREATIVE")!!)
        set(value) = tag.set("visitorGameMode", value.name)

    fun reviews(): List<Pair<String, Boolean>> = arrayListOf<Pair<String, Boolean>>().apply {
        val configuration = tag.getConfigurationSection("review") ?: return@apply
        configuration.getKeys(false).forEach {
            add(it to configuration.getBoolean(it, true))
        }
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

    fun partners(): List<String> = tag.getStringList("partners")
    fun isPartner(player: Player) = partners().contains(player.name)

    fun addPartner(who: ServerPlayer): Boolean {
        if (who == owner)
            return false
        val r = arrayListOf<String>()
        r.addAll(tag.getStringList("partners"))
        val name = who.name
        if (name != null) {
            if (r.contains(name))
                return false
            r.add(name)
            tag.set("partners", r)
            return true
        } else {
            return false
        }
    }

    fun removePartner(who: ServerPlayer): Boolean {
        if (who == owner)
            return false
        val r = arrayListOf<String>()
        r.addAll(tag.getStringList("partners"))
        val name = who.name
        if (name != null) {
            if (!r.remove(name))
                return false
            tag.set("partners", r)
            return true
        } else {
            return false
        }
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

        val first = PlayerLobbyManager.boundary.first
        val last = PlayerLobbyManager.boundary.second

        val lowY = first.blockY smaller last.blockY
        val highY = first.blockY bigger last.blockY

        Bukkit.getScheduler().runTask(Base.pluginCore) { _ ->
            val from = CuboidRegion(
                BukkitWorld(Base.spawnWorld),
                BlockVector3.at(first.blockX, lowY, first.blockZ),
                BlockVector3.at(last.blockX, highY, last.blockZ)
            )
            val to = BlockVector3.at(fromX, lowY, fromZ)
            val session = WorldEdit.getInstance().newEditSession(from.world)
            Operations.complete(
                ForwardExtentCopy(session, from, BukkitWorld(Base.lobby), to).apply {
                    isCopyingBiomes = true
                    isCopyingEntities = true
                }
            )
            session.close()
            spawnPoint = PlayerLobbyManager.bedLocation.clone().subtract(last).add(Vector(fromX, last.blockY, fromZ)).toLocation(Base.lobby)
            tag.set("initialized", true)
            save()
            isInitializing = false
        }
    }

    fun visitBy(player: Player) {
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
                    val isOwner = uuid == owner.uuid
                    val isPartner = isPartner(player)
                    if (!isSurvivor && isOwner) {
                        // To make speeches
                        var delay = 20L
                        fun speak(order: Int, arg: String? = null, block: (String) -> Unit) {
                            val string =
                                if (arg != null) getter["lobby.speeches.$order", arg] else getter["lobby.speeches.$order"]
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
                    } else if (!isOwner && !isPartner) {
                        player.gameMode = visitorGameMode
                    } else if (isPartner) {
                        player.gameMode = GameMode.CREATIVE
                    }
                }

                if (player.uniqueId != owner.uuid)
                    views++
            }
        }
    }

    fun save() {
        tag.save(tagFile)
    }

    companion object {
        val length: Int
            get() {
                val first = PlayerLobbyManager.boundary.first.blockZ
                val last = PlayerLobbyManager.boundary.second.blockZ
                return (Math.floorDiv(abs(first - last) + 1, 16) + 1) * 16
            }
        val width: Int
            get() {
                val first = PlayerLobbyManager.boundary.first.blockX
                val last = PlayerLobbyManager.boundary.second.blockX
                return (Math.floorDiv(abs(first - last), 16) + 1) * 16
            }
    }
}