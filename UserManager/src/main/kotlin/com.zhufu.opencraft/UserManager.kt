package com.zhufu.opencraft

import com.zhufu.opencraft.Base.Extend.toPrettyString
import com.zhufu.opencraft.Base.spawnWorld
import com.zhufu.opencraft.events.*
import com.zhufu.opencraft.inventory.PaymentDialog
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

class UserManager : JavaPlugin(), Listener {
    private lateinit var boardLocation: Location

    override fun onEnable() {
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
                    callEvent(PlayerLoginEvent(player))
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

    @EventHandler
    fun onOpPrelogin(event: AsyncPlayerPreLoginEvent) {
        if (Bukkit.getOfflinePlayer(event.uniqueId).isOp && !WebInfo.users.containsKey(event.address)) {
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
        val info = Info(event.player)
        PlayerManager.add(info)
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
    fun onPlayerLogin(event: PlayerLoginEvent) {
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

    private fun Player.goto(player: Player) {
        val targetInfo = PlayerManager.findInfoByPlayer(player) ?: return
        val getter = getLangGetter(targetInfo)
        if (targetInfo.status == Info.GameStatus.MiniGaming) {
            player.error(getter["user.error.invitationInGame"])
            return
        }
        if (targetInfo.status == Info.GameStatus.Observing) {
            player.error(getter["user.error.invitationInObser"])
            return
        }

        val thisInfo = PlayerManager.findInfoByPlayer(this) ?: return
        if (thisInfo.status == Info.GameStatus.InLobby) {
            thisInfo.inventory.create("survivor").set("location", player.location)
            ServerCaller["SolvePlayerLogin"]!!(listOf(thisInfo))
        } else {
            teleport(player)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (command.name == "user") {
            if (sender !is Player) {
                sender.error(Language.getDefault("command.error.playerOnly"))
                return true
            }
            if (args.isEmpty())
                return false
            val getter = getLangGetter(PlayerManager.findInfoByPlayer(sender))

            fun login(): Boolean {
                val info = PlayerManager.findInfoByPlayer(sender) ?: return false
                when {
                    args.size < 2 -> {
                        sender.error(getter["command.error.usage"])
                        return false
                    }
                    info.isLogin -> {
                        sender.error(getter["user.error.alreadyLogin"])
                    }
                    else -> {
                        info.login(args[1])
                        if (info.isLogin) {
                            server.pluginManager.apply {
                                callEvent(PlayerLoginEvent(sender))
                            }
                        }
                    }
                }
                return true
            }

            when (args.first()) {
                "reg" -> {
                    when {
                        args.size < 3 -> {
                            sender.error(getter["command.error.usage"])
                            getCommand("user reg")?.usage?.let {
                                sender.sendMessage(it)
                            }
                        }
                        args[1] != args[2] -> sender.error(getter["user.error.pwdDismatch"])
                        else -> {
                            val info = PlayerManager.findInfoByPlayer(sender) ?: return true
                            if (!info.isRegistered) {
                                info.registerPlayer(args[1])
                                if (info.isRegistered) {
                                    server.pluginManager.callEvent(PlayerRegisterEvent(info))
                                }
                                return true
                            } else {
                                sender.error(getter["user.error.alreadyReg"])
                            }
                        }
                    }
                    return false
                }

                "log" -> return login()
                "l" -> return login()

                "pwd" -> {
                    val info = PlayerManager.findInfoByPlayer(sender) ?: return true
                    when {
                        !info.isLogin -> {
                            sender.error(getter["user.error.notLoginYet"])
                        }
                        args.size < 4 -> {
                            sender.error(getter["command.error.usage"])
                        }

                        args[1] != info.password
                        -> {
                            sender.error(getter["user.error.wrongOldPwd"])
                        }

                        args[2] != args[3]
                        -> {
                            sender.error(getter["user.error.pwdDismatch"])
                        }
                        else -> {
                            info.password = args[2]
                            sender.info(getter["user.pwd.changed"])
                            return true
                        }
                    }
                    return false
                }

                "save" -> {
                    val info = PlayerManager.findInfoByPlayer(sender)
                    if (info == null) {
                        sender.error("player.error.unknown")
                        getCommand("user save")?.usage?.let {
                            sender.sendMessage(it)
                        }
                        return true
                    }
                    if (args.size == 2 && args[1] == "back") {
                        PaymentDialog(
                            sender,
                            SellingItemInfo(
                                ItemStack(Material.ENDER_PEARL)
                                    .also { itemStack ->
                                        itemStack.itemMeta = itemStack.itemMeta!!.also {
                                            it.setDisplayName(TextUtil.info(getter["ui.tpToSpawn"]))
                                        }
                                    },
                                3, 1
                            ), TradeManager.getNewID(), this
                        )
                            .setOnPayListener { success ->
                                if (success) {
                                    val dest = try {
                                        info.tag.getSerializable("surviveSpawn", Location::class.java)
                                    } catch (e: Exception) {
                                        sender.sendMessage(
                                            arrayOf(
                                                TextUtil.error(getter["user.error.spawnpointNotFound"]),
                                                getter["gameWarn"]
                                            )
                                        )
                                        return@setOnPayListener false
                                    }
                                    val event =
                                        PlayerTeleportedEvent(sender, sender.location, dest)
                                    server.pluginManager.callEvent(event)
                                    if (!event.isCancelled) {
                                        sender.info(getter["user.tpToSpawn"])
                                        sender.teleport(dest!!)
                                    } else {
                                        return@setOnPayListener false
                                    }
                                } else {
                                    sender.error(getter["trade.error.poor"])
                                }
                                true
                            }
                            .setOnCancelListener {
                                sender.info(getter["user.teleport.cancelled"])
                            }
                            .show()
                    } else {
                        sender.error(getter["command.error.usage"])
                    }
                }

                "observe" -> {
                    if (args.size < 2) {
                        sender.error(getter["command.error.usage"])
                        return false
                    }
                    val obPlayer = server.getPlayer(args[1])
                    if (obPlayer == null) {
                        sender.error(getter["command.error.playerNotFound"])
                        return true
                    }

                    Bukkit.getPluginManager().callEvent(PlayerObserveEvent(sender, obPlayer))
                }
                "deobserve" -> {
                    if (PlayerManager.findInfoByPlayer(sender)?.status == Info.GameStatus.Observing)
                        Bukkit.getPluginManager().callEvent(PlayerDeobserveEvent(sender))
                    else
                        sender.error(getter["user.error.nonObserving"])
                }

                "goto" -> {
                    if (args.size < 2) {
                        sender.error(getter["command.error.usage"])
                        getCommand("user goto")?.usage?.let {
                            sender.sendMessage(it)
                        }
                        return false
                    }
                    val info = PlayerManager.findInfoByPlayer(sender)
                    if (info == null) {
                        sender.error(getter["player.error.unknown"])
                        return true
                    }

                    fun sendGotoRequest(player: String): Boolean {
                        if (player == sender.name) return false
                        val player1 = server.getPlayer(player) ?: return false
                        val target = PlayerManager.findInfoByPlayer(player1)
                        if (target == null) {
                            sender.error(getter["command.error.playerNotFound"])
                            return true
                        }
                        if (target.gotoRequests.any { it.requester == sender && !it.isAccepted && !it.isTimeOut }) {
                            sender.error(getter["user.error.invitationAlreadySent"])
                            return true
                        }
                        target.gotoRequests.add(Info.GotoRequest(sender, 3 * 60 * 1000L))
                        target.gotoRequests.removeIf { it.isTimeOut }

                        val getterR = getLangGetter(PlayerManager.findInfoByPlayer(player1))
                        player1.info(getterR["user.invitation.receive", sender.name])
                        player1.tip(getterR["user.invitation.toAccept", if (target.gotoRequests.size == 1) "" else ' ' + sender.name])
                        return true
                    }

                    fun gotoCheckpoint(id: String): Boolean {
                        val point = info.checkpoints.firstOrNull { it.name == id } ?: return false
                        PaymentDialog(
                            sender,
                            SellingItemInfo(
                                ItemStack(Material.ENDER_PEARL)
                                    .also {
                                        it.itemMeta = it.itemMeta!!.also { itemMeta ->
                                            itemMeta.setDisplayName(TextUtil.info(getter["ui.teleport"]))
                                        }
                                    },
                                3,
                                1
                            )
                            , TradeManager.getNewID(), this
                        )
                            .setOnPayListener { success ->
                                if (success) {
                                    val event = PlayerTeleportedEvent(
                                        sender,
                                        sender.location,
                                        point.location
                                    )
                                    server.pluginManager.callEvent(event)
                                    if (!event.isCancelled) {
                                        sender.teleport(point.location)
                                        sender.success(getter["user.checkpoint.tpSucceed"])
                                    } else {
                                        return@setOnPayListener false
                                    }
                                } else {
                                    sender.error(getter["user.error.noSoManyCoins"])
                                }
                                true
                            }
                            .setOnCancelListener {
                                sender.info(getter["user.teleport.cancelled"])
                            }
                            .show()
                        return true
                    }
                    if (args[1].startsWith("player/")) {
                        if (!sendGotoRequest(args[1].substring(args[1].indexOf('/') + 1))) sender.error(getter["command.error.playerNotFound"])
                    } else if (args[1].startsWith("checkpoint/")) {
                        if (!gotoCheckpoint(args[1].substring(args[1].indexOf('/') + 1))) sender.error(getter["user.error.checkpointNotFound"])
                    } else {
                        if (!sendGotoRequest(args[1]) && !gotoCheckpoint(args[1])) {
                            sender.error(getter["user.error.checkpointOrPlayerNotFound"])
                        }
                    }
                }

                "accept" -> {
                    val info = PlayerManager.findInfoByPlayer(sender)
                    if (info == null) {
                        sender.error(getter["player.unknown"])
                        return true
                    }

                    if (info.status != Info.GameStatus.Surviving) {
                        sender.error(getter["user.error.invitationUnacceptable"])
                        return true
                    }

                    info.gotoRequests.removeIf { it.isTimeOut }
                    if (args.size == 1) {
                        when {
                            info.gotoRequests.isEmpty() -> {
                                sender.info(getter["user.error.noInvitations"])
                            }
                            info.gotoRequests.size > 1 -> {
                                sender.sendMessage(
                                    arrayOf(
                                        TextUtil.error(getter["user.error.moreThanOneInvitations.1"]),
                                        TextUtil.tip(getter["user.error.moreThanOneInvitations.2"]),
                                        TextUtil.info(getter["user.error.moreThanOneInvitations.3"])
                                    )
                                )
                                val sb = StringBuilder()
                                info.gotoRequests.forEach { sb.append("${it.requester.name}, ") }
                                sb.delete(sb.length - 2, sb.length)
                                sender.sendMessage(sb.toString())
                            }
                            else -> {
                                info.gotoRequests.first()
                                    .also { it.isAccepted = true }
                                    .requester.goto(sender)
                                info.gotoRequests.clear()
                            }
                        }
                    } else if (args.size > 1) {
                        val target = server.getPlayer(args[1])
                        if (target == null) {
                            sender.error(getter["command.playerNotFound"])
                            Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
                                info.gotoRequests.removeIf { it.requester.name == args[1] }
                            }
                            return true
                        }
                        val request = info.gotoRequests.firstOrNull { it.requester == target }
                        if (request == null) {
                            sender.error(getter["user.error.noSuchInvitation"])
                            return true
                        }

                        target.goto(sender)
                        request.isAccepted = true
                        info.gotoRequests.remove(request)
                    }
                }

                "death" -> {
                    if (args.size != 2) {
                        sender.error(getter["command.error.usage"])
                        getCommand("user death")?.usage?.let {
                            sender.sendMessage(it)
                        }
                        return false
                    }
                    fun Location.toPrettyString(): String =
                        "${this.world!!.name}(${this.blockX},${this.blockY},${this.blockZ})"
                    when (args[1]) {
                        "check" -> {
                            val info = PlayerManager.findInfoByPlayer(sender)
                            if (info == null) {
                                sender.error(getter["player.unknown"])
                                return true
                            }
                            sender.sendMessage(
                                TextUtil.getColoredText(
                                    "------${getter["user.lastDeath.title"]}------",
                                    TextUtil.TextColor.GOLD,
                                    false,
                                    false
                                )
                            )

                            if (info.tag.isSet("lastDeath")) {
                                val time = info.tag.getLong("lastDeath.time", -1)
                                val location = info.tag.getSerializable("lastDeath.location", Location::class.java)
                                val reason = info.tag.getString("lastDeath.reason", "")
                                sender.sendMessage(
                                    arrayOf(
                                        "${getter["user.lastDeath.time"]}: ${if (time == -1L) "null" else time.toString()}",
                                        "${getter["user.lastDeath.location"]}: ${location?.toPrettyString() ?: "null"}",
                                        "${getter["user.lastDeath.reason"]}: ${if (reason!!.isEmpty()) "null" else reason}"
                                    )
                                )
                            } else {
                                sender.info(getter["user.lastDeath.noRecord"])
                            }
                        }
                        "back" -> {
                            val info = PlayerManager.findInfoByPlayer(sender)
                            if (info == null) {
                                sender.error(getter["player.error.unknown"])
                                return true
                            }
                            val t = info.tag.getSerializable("lastDeath.location", Location::class.java)
                            if (t == null) {
                                sender.error(getter["user.lastDeath.noRecord"])
                                return true
                            }
                            val prise = Game.env.getInt("backToDeathPrise")
                            sender.sendMessage(
                                arrayOf(
                                    TextUtil.info(getter["user.lastDeath.last", t.toPrettyString()]),
                                    TextUtil.tip(getter["user.lastDeath.toGo", server.getPluginCommand("user bd")!!.usage, prise])
                                )
                            )
                        }
                    }
                }
                "bd" -> {
                    val info = PlayerManager.findInfoByPlayer(sender)
                    if (info == null) {
                        sender.error(getter["player.unknown"])
                        return true
                    }
                    val t = info.tag.getSerializable("lastDeath.location", Location::class.java)
                    if (t == null) {
                        sender.error(getter["user.lastDeath.noRecord"])
                        return true
                    }
                    val prise = Game.env.getInt("backToDeathPrise")
                    if (info.currency < prise) {
                        sender.error(getter["user.error.noSoManyCoins"])
                        return true
                    }

                    val event = PlayerTeleportedEvent(sender, sender.location, t)
                    server.pluginManager.callEvent(event)
                    if (!event.isCancelled) {
                        sender.teleport(t)
                        info.currency -= prise
                        sender.info(getter["user.lastDeath.goTo", prise])
                    }

                    info.tag.set("lastDeath", null)
                }

                "saveas" -> {
                    if (args.size < 2) {
                        sender.error(getter["command.error.usage"])
                        getCommand("user saveas")?.usage?.let {
                            sender.sendMessage(it)
                        }
                        return true
                    }
                    val info = PlayerManager.findInfoByPlayer(sender)
                    if (info == null) {
                        sender.error(getter["player.error.unknown"])
                        return true
                    }
                    if (sender.world == Base.lobby) {
                        sender.error(getter["command.error.world"])
                        return true
                    }

                    val id = args[1]
                    if (info.checkpoints.any { it.name == id }) {
                        sender.error(getter["user.checkpoint.alreadyExist", id])
                    } else {
                        val point = OfflineInfo.CheckpointInfo(sender.location, id)
                        info.checkpoints.add(point)
                        sender.info(getter["user.checkpoint.saved", point.location.toPrettyString(), id])
                        sender.tip(getter["user.checkpoint.toGo", id])
                    }
                }
                "delsave" -> {
                    if (args.size < 2) {
                        sender.error(getter["command.error.usage"])
                        getCommand("user delsave")?.usage?.let {
                            sender.sendMessage(it)
                        }
                        return true
                    }
                    val info = PlayerManager.findInfoByPlayer(sender)
                    if (info == null) {
                        sender.error(getter["player.error.unknown"])
                        return true
                    }
                    if (!info.checkpoints.removeIf { it.name == args[1] }) {
                        sender.error(getter["user.checkpoint.notFound"])
                    } else {
                        sender.info(getter["user.checkpoint.del", args[1]])
                    }
                }

                "lang" -> {
                    if (args.size < 2) {
                        sender.error(getter["command.error.usage"])
                        getCommand("user lang")?.usage?.let {
                            sender.sendMessage(it)
                        }
                        return true
                    }
                    val target = args[1]
                    val code = Language.getCodeByName(target)
                        ?: Language.languages.firstOrNull { it.getString("info.code") == target }?.getString("info.code")
                    if (code == null) {
                        sender.error(getter["user.error.langNotFound"])
                        return true
                    }
                    val info = PlayerManager.findInfoByPlayer(sender)
                    if (info == null) {
                        sender.error(getter["player.error.unknown"])
                        return true
                    }
                    info.userLanguage = code
                    sender.success(getter["user.langChangeSucceed", code])
                }

                "transfer" -> {
                    if (args.size < 3) {
                        sender.error(getter["command.error.usage"])
                        getCommand("user transfer")?.usage?.let {
                            sender.sendMessage(it)
                        }
                        return true
                    }

                    val info = PlayerManager.findInfoByPlayer(sender)
                    if (info == null) {
                        sender.error(getter["player.error.unknown"])
                        return true
                    }

                    val oldPwd = args[1]
                    val pwd = args[2]

                    if (info.password != pwd) {
                        sender.error(getter["user.login.failed"])
                        return true
                    }

                    var result: OfflineInfo? = null
                    var errorSent = false
                    OfflineInfo.forEach {
                        if (it.password != oldPwd || it.uuid == sender.uniqueId)
                            return@forEach
                        if (result == null) {
                            result = it
                        } else if (!errorSent) {
                            sender.error(getter["user.transfer.overOne"])
                            errorSent = true
                        }
                    }
                    if (errorSent) {
                        return true
                    }
                    if (result == null) {
                        sender.error(getter["user.transfer.zero"])
                        return true
                    }
                    if (result!!.isOnline) {
                        sender.error(getter["user.transfer.online"])
                        return true
                    }
                    info.copyFrom(result!!)
                        .also { list ->
                            if (list.isNotEmpty()) {
                                val str = buildString {
                                    list.forEach {
                                        append(it)
                                        append(',')
                                    }
                                    deleteCharAt(lastIndex)
                                }
                                sender.error(getter["user.error.whileLoading", str])
                                sender.error(getter["user.transfer.failure", result!!.uuid])
                            } else {
                                sender.success(getLang(info, "user.transfer.success"))
                            }
                        }
                    result!!.delete()
                }

                "prefer" -> {
                    if (args.size < 2) {
                        sender.error(getter["command.error.usage"])
                        getCommand("user prefer")?.usage?.let {
                            sender.sendMessage(it)
                        }
                    } else {
                        val info = sender.info()
                        if (info == null) {
                            sender.error(getter["player.error.unknown"])
                        } else {
                            val parName = args[1]
                            val member = info.preference::class.memberProperties.firstOrNull {
                                it.name == parName && it.visibility == KVisibility.PUBLIC
                            } as KMutableProperty1<PlayerPreference, *>?
                            when {
                                member == null -> sender.error(getter["user.error.noSuchPreference", parName])
                                args.size == 2 -> {
                                    sender.info(
                                        "$parName -> ${
                                        if (member.returnType.classifier == Boolean::class) {
                                            if (member.get(info.preference) as Boolean) "on" else "off"
                                        } else {
                                            member.get(info.preference)
                                        }
                                        }"
                                    )
                                }
                                else -> {
                                    val section = args[2]
                                    fun <T> castTo() = member as KMutableProperty1<PlayerPreference, T>
                                    if (member.returnType.classifier == Boolean::class) {
                                        castTo<Boolean>().set(
                                            info.preference,
                                            (when (section) {
                                                "off" -> false
                                                "on" -> true
                                                else -> {
                                                    sender.error(getter["command.error.typeError", "on | off"])
                                                    return true
                                                }
                                            })
                                        )
                                    } else if (member.returnType.classifier == Int::class) {
                                        val num = section.toIntOrNull()
                                        if (num == null) {
                                            sender.error(getter["command.error.typeError", "Integer"])
                                        } else {
                                            castTo<Int>().set(
                                                info.preference,
                                                num
                                            )
                                        }
                                    } else if (member.returnType.classifier == String::class) {
                                        castTo<String>().set(
                                            info.preference,
                                            section
                                        )
                                    } else return false
                                    sender.success(getter["user.preferred", parName, section])
                                }
                            }
                        }
                    }
                }

                else -> {
                    sender.error(getter["command.error.usage"])
                    return false
                }
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (command.name == "user") {
            if (sender !is Player) {
                return mutableListOf()
            }
            if (args.isNotEmpty()) {
                val first = args.first()
                val commands = mutableListOf(
                    "log",
                    "reg",
                    "pwd",
                    "save",
                    "observe",
                    "deobserve",
                    "goto",
                    "accept",
                    "death",
                    "bd",
                    "saveas",
                    "delsave",
                    "help",
                    "lang",
                    "transfer",
                    "prefer"
                )

                if (args.size == 1) {
                    return if (args.first().isEmpty())
                        commands
                    else {
                        val r = ArrayList<String>()
                        commands.forEach { if (it.startsWith(args.first())) r.add(it) }
                        r
                    }
                }
                //Else
                when (first) {
                    "save" -> {
                        if (args.size == 2) {
                            return mutableListOf("back")
                        }
                    }
                    "observe" -> {
                        val result = ArrayList<String>()
                        server.onlinePlayers.forEach {
                            if (it.uniqueId != sender.uniqueId)
                                result.add(it.name)
                        }
                        if (args.size == 2) {
                            return if (args[1].isEmpty())
                                result.toMutableList()
                            else {
                                val result2 = ArrayList<String>()
                                result.forEach { if (it.startsWith(args[1])) result2.add(it) }
                                result2.toMutableList()
                            }
                        }
                    }
                    "goto" -> {
                        val info = PlayerManager.findInfoByPlayer(sender)
                        if (info == null) {
                            sender.error(Language.getDefault("player.error.unknown"))
                            return mutableListOf()
                        }
                        val result = ArrayList<String>()
                        server.onlinePlayers.forEach {
                            if (it.uniqueId != sender.uniqueId)
                                result.add(it.name)
                        }
                        info.checkpoints.forEach {
                            val index = result.indexOf(it.name)
                            if (index != -1) {
                                result[index] = "player/${it.name}"
                                result.add("checkpoint/${it.name}")
                            } else result.add(it.name)
                        }
                        if (args.size == 2) {
                            return if (args[1].isEmpty()) {
                                result.toMutableList()
                            } else {
                                val result2 = ArrayList<String>()
                                result.forEach {
                                    if (it.startsWith(args[1])) result2.add(it)
                                    else {
                                        val index = it.indexOf('/')
                                        if (index != -1 && it.substring(index + 1).startsWith(args[1])) {
                                            result2.add(it)
                                        }
                                    }
                                }
                                result2.toMutableList()
                            }
                        }
                    }
                    "accept" -> {
                        val preResult = ArrayList<String>()
                        PlayerManager.findInfoByPlayer(sender)!!.gotoRequests.forEach {
                            if (!it.isTimeOut && !it.isAccepted) preResult.add(
                                it.requester.name
                            )
                        }
                        if (args.size == 2) {
                            return if (args[1].isEmpty())
                                preResult.toMutableList()
                            else {
                                val result = ArrayList<String>()
                                preResult.forEach { if (it.startsWith(args[1])) result.add(it) }
                                result.toMutableList()
                            }
                        }
                    }
                    "death" -> {
                        val arg = arrayListOf("back", "check")
                        if (args.size == 2) {
                            if (args[1].isNotEmpty()) {
                                val result = ArrayList<String>()
                                arg.forEach { if (it.startsWith(arg[1])) result.add(it) }
                                return result
                            }
                            return arg.toMutableList()
                        }
                    }
                    "delsave" -> {
                        val info = PlayerManager.findInfoByPlayer(sender)
                        if (info == null) {
                            sender.error(Language.getDefault("player.error.unknown"))
                            return mutableListOf()
                        }
                        val r = ArrayList<String>()
                        info.checkpoints.forEach { r.add(it.name) }
                        if (args.size == 2) {
                            return if (args[1].isEmpty()) {
                                r
                            } else {
                                val result = ArrayList<String>()
                                r.forEach { if (it.startsWith(args[1])) result.add(it) }
                                result
                            }
                        }
                    }
                    "lang" -> {
                        if (args.size == 2) {
                            val langs = ArrayList<String>()
                            Language.languages.forEach { langs.add(it.getString("info.name")!!) }
                            return if (args.last().isEmpty()) {
                                langs
                            } else {
                                langs.asSequence().filter { it.startsWith(args[1]) }.toMutableList()
                            }
                        }
                    }
                    "prefer" -> {
                        val info = sender.info()
                        if (args.size >= 2 && info != null) {
                            if (args.size == 2) {
                                val r = mutableListOf<String>().apply {
                                    info.preference::class.memberProperties.forEach {
                                        if (it.visibility == KVisibility.PUBLIC)
                                            add(it.name)
                                    }
                                }
                                return if (args[1].isEmpty()) r
                                else r.filter { it.startsWith(args[1]) }.toMutableList()
                            } else {
                                val member =
                                    info.preference::class.memberProperties.firstOrNull {
                                        it.name == args[1] && it.visibility == KVisibility.PUBLIC
                                    }
                                            as KMutableProperty1<PlayerPreference, *>?

                                return if (member != null && member.returnType.classifier == Boolean::class)
                                    mutableListOf("on", "off")
                                else
                                    mutableListOf()
                            }
                        }
                    }
                }
            }
        }
        return mutableListOf()
    }
}