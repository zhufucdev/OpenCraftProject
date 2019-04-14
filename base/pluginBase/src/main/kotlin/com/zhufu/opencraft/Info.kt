package com.zhufu.opencraft

import com.zhufu.opencraft.util.CommonPlayerStream
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.io.File
import java.util.*
import kotlin.concurrent.timer

class Info(val player: Player) : OfflineInfo(player.uniqueId,true), PluginBase, ChatInfo {
    companion object {
        val mList = ArrayList<Info>()
        lateinit var plugin: Plugin

        fun findByPlayer(player: Player) = mList.firstOrNull { it.player == player }
        fun findByPlayer(uuid: UUID) = mList.firstOrNull { it.player.uniqueId == uuid }
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
        MiniGaming, InLobby, Surviving, Observing, InTutorial, Building
    }

    var status: GameStatus = GameStatus.InLobby
    override var doNotTranslate = false
    override val displayName: String = player.displayName
    override val targetLang: String
        get() = userLanguage
    override val playerStream: PlayerStream
        get() = CommonPlayerStream(player)
    override val id: String
        get() = name?:"unknown"

    val gotoRequests = ArrayList<GotoRequest>()

    var inventory: DualInventory = DualInventory(player, this)

    var isRegistered: Boolean = password != null
        private set
    var isLogin: Boolean = false
        private set

    var savedAddress: String?
        get() = tag.getString("address")
        private set(value) {
            tag.set("address", value)
            saveTag()
        }

    fun copyFrom(target: OfflineInfo): List<String> {
        val failureList = ArrayList<String>()
        val oldInventory = File("plugins${File.separatorChar}inventories${File.separatorChar}${target.uuid}")
        if (oldInventory.exists()) {
            inventory.present.save()

            try {
                this.inventory.delete()
                oldInventory.renameTo(File("plugins${File.separatorChar}inventories${File.separatorChar}${this.uuid}"))
                inventory = DualInventory(player, this)
                inventory.create(DualInventory.RESET).load()
            } catch (e: Exception) {
                failureList.add("inventories/${e::class.simpleName} ${e.cause}")
            }
            val method = ServerCaller["ChangeChunkOwner"]
            if (method != null)
                method.invoke(listOf(target.uuid,this.uuid))
            else
                failureList.add("chunk/server caller found nothing.")

        }

        try {
            this.tag = target.tag
            saveTag()
        } catch (e: Exception) {
            failureList.add("tag/${e::class.simpleName}: ${e.message}")
        }

        return failureList
    }

    fun login(pwd: String) {
        isLogin = true
        val getter = getLangGetter(this as ChatInfo)
        when {
            pwd == password -> try {
                inventory.create(DualInventory.RESET).load()
                player.removePotionEffect(PotionEffectType.BLINDNESS)
                player.isInvulnerable = false
                Bukkit.getOnlinePlayers().forEach { it.showPlayer(plugin, player) }

                player.info(getter["user.login.success"])
            } catch (e: Exception) {
                player.sendMessage(TextUtil.printException(e))

                isLogin = false
                isRegistered = false
                player.tip(getter["user.error.toRegister", plugin?.server?.getPluginCommand("user reg")?.usage]);
            }
            password == null -> {
                isLogin = false
                player.error(getter["user.toRegister"])
            }
            else -> {
                isLogin = false
                player.error(getter["user.login.failed"])
            }
        }
        if (isLogin) {
            savedAddress = player.address!!.hostName
            player.resetTitle()
        }
    }

    private fun login() {
        player.removePotionEffect(PotionEffectType.BLINDNESS)
        player.walkSpeed = 0.2f
        player.flySpeed = 0.1f
        player.gameMode = GameMode.SURVIVAL
        player.teleport(Base.spawnWorld.spawnLocation)
        player.isInvulnerable = false

        plugin.server.onlinePlayers.forEach { it.showPlayer(plugin, player) }

        savedAddress = player.address!!.hostName
        isLogin = true
        player.resetTitle()
    }

    fun logout(borderLocation: Location) {
        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, Math.pow(10.toDouble(), 10.toDouble()).toInt(), 128, false, false))
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

    fun registerPlayer(pwd: String) {
        password = pwd
        isRegistered = true

        login()
        saveServerID()
    }

    fun saveServerID() {
        inventory.present.save()
        saveTag()
    }

    override fun destroy() {
        super.destroy()
        mList.removeAll { it.uuid == uuid }
    }
}