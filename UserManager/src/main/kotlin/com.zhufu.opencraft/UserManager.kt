package com.zhufu.opencraft

import com.zhufu.opencraft.Base.spawnWorld
import com.zhufu.opencraft.events.UserLoginEvent
import com.zhufu.opencraft.events.PlayerLogoutEvent
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import com.zhufu.opencraft.player_community.Friend
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

class UserManager : JavaPlugin(), Listener {
    private lateinit var boardLocation: Location

    override fun onEnable() {
        getCommand("user")!!.apply {
            val executor = UserCommandExecutor(this@UserManager)
            setExecutor(executor)
            tabCompleter = executor
        }
        getCommand("friend")!!.apply {
            val executor = FriendCommandExecutor(this@UserManager)
            setExecutor(executor)
            tabCompleter = executor
        }

        server.pluginManager
            .also { it.registerEvents(PlayerManager, this) }
            .registerEvents(this, this)

        server.onlinePlayers.forEach {
            val info = Info(it)
            if (info.password != null)
                info.login(info.password!!)
            PlayerManager.add(info)
        }

        Bukkit.getScheduler().runTask(this) { _ ->
            logger.info("Creating spwan chunk blocks.")
            val chunk = spawnWorld.getChunkAt(0, 0)
            spawnWorld.loadChunk(chunk)

            for (x in 0..15) {
                for (z in 0..15) {
                    chunk.getBlock(x, 1, z).type = Material.BARRIER
                    if (x % 15 == 0 || z % 15 == 0) {
                        chunk.getBlock(x, 2, z).type = Material.BARRIER
                        chunk.getBlock(x, 3, z).type = Material.BARRIER
                    }
                }
            }
            boardLocation = chunk.getBlock(8, 3, 8).location
            spawnWorld.spawnLocation = boardLocation

            logger.info("Chunk created.")
        }

        ServerCaller["SolvePlayerLogin"] = {
            val info = (it.firstOrNull()
                ?: throw IllegalArgumentException("This call must be given at least one Info parameter.")) as Info
            if (info.isUserLanguageSelected)
                Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
                    showLoginMsg(info)
                }
            else {
                info.logout(boardLocation)
                info.player.info(Language.getDefault("user.toSelectLang"))
                Language.printLanguages(info.player)
            }
        }
    }

    private fun showLoginMsg(info: Info) {
        val player = info.player
        logger.info("${player.name} login with address: ${player.address!!.hostName}")
        if (player.address!!.hostName == info.savedAddress) {
            player.info(getLang(info, "user.loginWithIP"))
            Bukkit.getScheduler().runTask(this) { _ ->
                info.login(info.password!!)
                server.pluginManager.apply {
                    callEvent(UserLoginEvent(player))
                }
            }
            player.resetTitle()
        } else {
            val getter = getLangGetter(info)
            if (!info.isRegistered) {
                player.sendTitle(
                    TextUtil.info(getter["user.reg1"]),
                    TextUtil.tip(getter["user.reg2", server.getPluginCommand("user reg")!!.usage]),
                    10,
                    16 * 1000,
                    10
                )
            } else if (!info.isLogin) {
                player.sendTitle(
                    TextUtil.info(getter["user.login1"]),
                    TextUtil.tip(getter["user.login2", server.getPluginCommand("user log")!!.usage]),
                    10,
                    16 * 1000,
                    10
                )
            }

            Bukkit.getScheduler().runTaskLater(
                this,
                { _ ->
                    if (info.player.isOnline && !info.isLogin && info.player.world == spawnWorld) {
                        logger.info("${player.name} may be kicked because of timeout.")
                        player.kickPlayer(TextUtil.error(getter["user.login.timeout"]))
                    }
                }, if (info.isRegistered) 30 * 25L else 60 * 25L
            )
            Bukkit.getScheduler().runTask(this) { _ ->
                info.logout(boardLocation)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun playerLogin(event: org.bukkit.event.player.PlayerLoginEvent) {
        val info = Info(event.player)
        PlayerManager.add(info)
    }

    @EventHandler
    fun onOpPrelogin(event: AsyncPlayerPreLoginEvent) {
        if (!Game.env.getBoolean("debug")
            && Bukkit.getOfflinePlayer(event.uniqueId).isOp
            && !WebInfo.users.containsKey(event.address)
        ) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                OfflineInfo.findByUUID(event.uniqueId).let {
                    if (it != null) {
                        getLang(it, "player.error.opNotLoginOnWeb").toErrorMessage()
                    } else {
                        "管理员必须先在网页登录，再在游戏中登录"
                    }
                }
            )
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player) ?: return
        event.joinMessage = ""
        if (!info.isUserLanguageSelected) {
            info.doNotTranslate = true
            info.logout(boardLocation)
            info.player.info(Language.getDefault("user.toSelectLang"))
            Language.printLanguages(info.player)
        } else {
            info.logout()
            Bukkit.getPluginManager()
                .callEvent(PlayerTeleportedEvent(event.player, null, PlayerLobbyManager[info].spawnPoint))
            Bukkit.getScheduler().runTaskLater(this, { _ ->
                Bukkit.getPluginManager().callEvent(PlayerLogoutEvent(info, false))
            }, 5)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        event.quitMessage = ""
        val info = PlayerManager.findInfoByPlayer(event.player) ?: return
        if (info.isLogin) {
            broadcast("player.left", TextUtil.TextColor.YELLOW, info.player.name)
        }
        info.destroy()
    }

    @EventHandler
    fun onPlayerLogout(event: PlayerLogoutEvent) {
        if (event.showMesage()) broadcast("player.left", TextUtil.TextColor.YELLOW, event.info.player.name)
    }

    @EventHandler
    fun onPlayerLogin(event: UserLoginEvent) {
        broadcast("player.join", TextUtil.TextColor.AQUA, event.player.displayName)
    }

    @EventHandler
    fun onPlayerPrelogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            return
        Bukkit.getScheduler().runTaskLater(this, { _ ->
            if (Bukkit.getPlayer(event.uniqueId) == null) {
                broadcast("player.warn.badNet", TextUtil.TextColor.AQUA, event.name)
            }
        }, 30)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player) ?: return
        if (!info.isUserLanguageSelected) {
            event.isCancelled = true
            val code = Language.getCodeByName(event.message)
                ?: if (TextUtil.detectString(event.message) == TextUtil.StringDetectResult.Int) Language.getCodeByOrder(
                    event.message.toInt()
                ) else null
            if (code != null) {
                info.userLanguage = code
                info.doNotTranslate = false
                Bukkit.getScheduler().runTask(this) { _ ->
                    info.inventory.create(DualInventory.RESET).load()
                    Bukkit.getPluginManager()
                        .callEvent(
                            PlayerTeleportedEvent(
                                event.player,
                                spawnWorld.spawnLocation,
                                PlayerLobbyManager[info].spawnPoint
                            )
                        )
                }
            } else {
                event.player.error(Language.getDefault("user.error.langNotFound"))
                Language.printLanguages(event.player)
            }
        }
    }

    @EventHandler
    fun onServerReload(event: ServerReloadEvent) {
        Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
            Friend.saveAll()
        }
    }
}