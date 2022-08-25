package com.zhufu.opencraft.data

import com.zhufu.opencraft.*
import com.zhufu.opencraft.api.ChatInfo
import com.zhufu.opencraft.api.ServerCaller
import com.zhufu.opencraft.player_community.PlayerOutputStream
import com.zhufu.opencraft.util.BukkitPlayerOutputStream
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.TextUtil
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.io.File
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.pow

class Info(val player: Player) : OfflineInfo(player.uniqueId, true), ChatInfo {
    companion object {
        val cache = ArrayList<Info>()
        lateinit var plugin: Plugin

        fun findByName(name: String) = cache.firstOrNull { it.name == name }
        fun findByPlayer(player: Player) = cache.firstOrNull { it.player == player }
        fun findByPlayer(uuid: UUID) = cache.firstOrNull { it.player.uniqueId == uuid }
    }

    class GotoRequest(val requester: Player, val timeLimit: Long) {
        init {
            timer("timeoutSender", initialDelay = timeLimit, period = 1) {
                if (!isAccepted)
                    requester.sendMessage(TextUtil.info(Language[requester, "user.error.invitationTimeout"]))
                this.cancel()
            }
        }

        private val creationTime = System.currentTimeMillis()
        val isTimeOut: Boolean
            get() = System.currentTimeMillis() - creationTime >= timeLimit
        var isAccepted = false

        override fun equals(other: Any?): Boolean {
            return other is GotoRequest && other.creationTime == this.creationTime && other.requester == this.requester && other.timeLimit == this.timeLimit
        }

        override fun hashCode(): Int {
            var result = requester.hashCode()
            result = 31 * result + timeLimit.hashCode()
            result = 31 * result + creationTime.hashCode()
            return result
        }
    }

    enum class GameStatus {
        MiniGaming, InLobby, Surviving, Observing, InTutorial, Building, Offline
    }

    /* Runtime Factors */
    var status: GameStatus = GameStatus.InLobby
    var outOfSpawn = false
    var isTerritoryInMessageShown = false
    var isTerritoryOutMessageShown = false
    var isSurveyRequestShown = false

    override var doNotTranslate = false
    override val displayName: String
        get() = player.name + if (nickname != null) ", $nickname" else ""
    override val targetLang: String
        get() = userLanguage
    override val playerOutputStream: PlayerOutputStream by lazy { BukkitPlayerOutputStream(player) }

    val gotoRequests = ArrayList<GotoRequest>()

    var isRegistered: Boolean = hasPassword
        private set
    var isLogin: Boolean = false
        private set
    var hasLogin: Boolean = false
        private set
    val isInBuilderMode get() = status == GameStatus.Building
    override var name: String?
        get() = doc.getString("name") ?: player.name
        set(value) {
            if (value == null) {
                doc.remove("name")
            } else {
                doc["name"] = value
            }
            update()
        }

    var savedAddress: String?
        get() = doc.getString("address")
        private set(value) {
            if (value == null) {
                doc.remove("address")
            } else {
                doc["address"] = value
            }
            update()
        }

    fun copyFrom(target: OfflineInfo): List<String> {
        val failureList = ArrayList<String>()
        val oldInventory = File("plugins${File.separatorChar}inventories${File.separatorChar}${target.uuid}")
        if (oldInventory.exists()) {
            inventory.present.sync()

            try {
                this.inventory.delete()
                oldInventory.renameTo(File("plugins${File.separatorChar}inventories${File.separatorChar}${this.uuid}"))
                inventory = DualInventory(player, this)
                inventory.getOrCreate(DualInventory.RESET).load()
            } catch (e: Exception) {
                failureList.add("inventories/${e::class.simpleName} ${e.cause}")
            }
            val method = ServerCaller["ChangeChunkOwner"]
            if (method != null)
                method.invoke(listOf(target.uuid, this.uuid))
            else
                failureList.add("chunk/server caller found nothing.")

            statics!!.copyFrom(target.statics!!)
        }

        try {
            doc.putAll(target.doc)
            update()
        } catch (e: Exception) {
            failureList.add("tag/${e::class.simpleName}: ${e.message}")
        }

        return failureList
    }

    fun login(pwd: String) {
        isLogin = true
        val getter = getLangGetter(this as ChatInfo)
        when {
            matchPassword(pwd) -> login()
            !hasPassword -> {
                isLogin = false
                player.error(getter["user.toRegister"])
            }

            else -> {
                isLogin = false
                player.error(getter["user.login.failed"])
            }
        }
    }

    fun login() {
        try {
            player.removePotionEffect(PotionEffectType.BLINDNESS)
            player.walkSpeed = 0.2f
            player.flySpeed = 0.1f
            player.gameMode = GameMode.SURVIVAL
            player.isInvulnerable = false

            plugin.server.onlinePlayers.forEach { it.showPlayer(plugin, player) }

            savedAddress = player.address!!.hostName
            isLogin = true
            hasLogin = true
            player.resetTitle()

            ServerCaller["SolvePlayerSurvive"]!!(listOf(player))

            player.info(getter()["user.login.success"])
        } catch (e: Exception) {
            player.sendMessage(*TextUtil.printException(e))

            isLogin = false
            isRegistered = false
            player.tip(
                player.info().getter()[
                        "user.error.toRegister",
                        plugin.server.getPluginCommand("user reg")?.usage
                ]
            )
        }
    }

    /**
     * Logout before starting survival
     */
    fun logout(borderLocation: Location) {
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.BLINDNESS,
                10.toDouble().pow(10.toDouble()).toInt(),
                128,
                false,
                false
            )
        )
        player.inventory.clear()
        player.walkSpeed = 0f
        player.exp = 0f
        player.foodLevel = 50
        player.gameMode = GameMode.ADVENTURE
        player.isInvulnerable = true
        player.teleport(borderLocation)

        plugin.server.onlinePlayers.forEach { it.hidePlayer(plugin, player) }

        isLogin = false
    }

    /**
     * Logout before getting into lobby
     */
    fun logout() {
        inventory.getOrCreate(DualInventory.RESET).load()

        status = GameStatus.InLobby
        isLogin = false
    }

    fun registerPlayer(pwd: String) {
        setPassword(pwd)
        isRegistered = true

        login()
    }

    override fun destroy() {
        inventory.present.sync()
        cache.removeAll { it.uuid == uuid }
        super.destroy()
    }
}