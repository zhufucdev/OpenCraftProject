package com.zhufu.opencraft

import com.zhufu.opencraft.Base.Extend.toPrettyString
import com.zhufu.opencraft.api.ServerCaller
import com.zhufu.opencraft.data.*
import com.zhufu.opencraft.events.*
import com.zhufu.opencraft.inventory.PaymentDialog
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toInfoMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

class UserCommandExecutor(private val plugin: UserManager) : TabExecutor {
    @Suppress("UNCHECKED_CAST")
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
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
                    Bukkit.getOnlinePlayers().forEach {
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

                    val map = arrayListOf<Pair<String, String>>()
                    val result = arrayListOf<String>()
                    Bukkit.getOnlinePlayers().forEach {
                        if (it != sender)
                            map.add("player" to it.name)
                    }
                    info.sharedCheckpoints.forEach {
                        val name = it.first.name ?: return@forEach
                        map.add(name to it.second.name)
                    }
                    info.checkpoints.forEach {
                        map.add("checkpoint" to it.name)
                    }

                    map.forEach { (key, value) ->
                        val index = result.indexOf(value)
                        if (index != -1) {
                            val old = map[index]
                            result[index] = "${old.first}/${old.second}"
                            result.add("$key/$value")
                        } else {
                            result.add(value)
                        }
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
        return mutableListOf()
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
            thisInfo.inventory.getOrCreate("survivor").set("location", player.location)
            ServerCaller["SolvePlayerLogin"]!!(listOf(thisInfo))
        } else {
            if (targetInfo.status == Info.GameStatus.InLobby) {
                thisInfo.apply {
                    inventory.getOrCreate(DualInventory.RESET).load()
                    status = Info.GameStatus.InLobby
                }
                PlayerLobbyManager[targetInfo].visitBy(this)
            }

            player.isInvulnerable = true
            teleport(player)

            Bukkit.getScheduler().runTaskLater(plugin, { _ ->
                player.isInvulnerable = false
            }, 20)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
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
                        Bukkit.getPluginManager().callEvent(
                            UserLoginEvent(
                                sender
                            )
                        )
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
                        plugin.getCommand("user reg")?.usage?.let {
                            sender.sendMessage(it)
                        }
                    }

                    args[1] != args[2] -> sender.error(getter["user.error.pwdDismatch"])
                    else -> {
                        val info = PlayerManager.findInfoByPlayer(sender) ?: return true
                        if (!info.isRegistered) {
                            info.registerPlayer(args[1])
                            if (info.isRegistered) {
                                Bukkit.getPluginManager().callEvent(PlayerRegisterEvent(info))
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

                    !info.matchPassword(args[1])
                    -> {
                        sender.error(getter["user.error.wrongOldPwd"])
                    }

                    args[2] != args[3]
                    -> {
                        sender.error(getter["user.error.pwdDismatch"])
                    }

                    else -> {
                        info.setPassword(args[2])
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
                    plugin.getCommand("user save")?.usage?.let {
                        sender.sendMessage(it)
                    }
                    return true
                }
                if (args.size == 2 && args[1] == "back") {
                    val dest = try {
                        info.survivalSpawn ?: throw IllegalArgumentException()
                    } catch (e: Exception) {
                        sender.error(getter["user.error.spawnpointNotFound"])
                        sender.warn(getter["gameWarn"])
                        return true
                    }
                    dest.chunk.load()
                    PaymentDialog(
                        sender,
                        SellingItemInfo(
                            ItemStack(Material.ENDER_PEARL)
                                .updateItemMeta<ItemMeta> {
                                    displayName(getter["ui.tpToSpawn"].toInfoMessage())
                                },
                            3, 1
                        ), TradeManager.getNewID(), plugin
                    )
                        .setOnPayListener { success ->
                            if (success) {

                                val event =
                                    PlayerTeleportedEvent(sender, sender.location, dest)
                                Bukkit.getPluginManager().callEvent(event)
                                if (!event.isCancelled) {
                                    sender.info(getter["user.tpToSpawn"])
                                    sender.teleport(dest)
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
                val obPlayer = Bukkit.getPlayer(args[1])
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
                    plugin.getCommand("user goto")?.usage?.let {
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
                    val player1 = Bukkit.getPlayer(player) ?: return false
                    val target = PlayerManager.findInfoByPlayer(player1)
                    if (target == null) {
                        sender.error(getter["command.error.playerNotFound"])
                        return true
                    }
                    if (info.friendships[target]?.let { it.isFriend && it.shareLocation } == true) {
                        sender.goto(player1)
                    } else {
                        if (target.gotoRequests.any { it.requester == sender && !it.isAccepted && !it.isTimeOut }) {
                            sender.error(getter["user.error.invitationAlreadySent"])
                            return true
                        }
                        target.gotoRequests.add(Info.GotoRequest(sender, 3 * 60 * 1000L))
                        target.gotoRequests.removeIf { it.isTimeOut }

                        val getterR = getLangGetter(PlayerManager.findInfoByPlayer(player1))
                        player1.info(getterR["user.invitation.receive", sender.name])
                        player1.tip(getterR["user.invitation.toAccept", if (target.gotoRequests.size == 1) "" else ' ' + sender.name])
                    }
                    return true
                }

                fun gotoCheckpoint(info: ServerPlayer, id: String): Boolean {
                    val point = info.checkpoints.firstOrNull { it.name == id } ?: return false
                    point.location.chunk.load()
                    PaymentDialog(
                        sender,
                        SellingItemInfo(
                            ItemStack(Material.ENDER_PEARL)
                                .updateItemMeta<ItemMeta> {
                                    displayName(getter["ui.teleport"].toInfoMessage())
                                },
                            3,
                            1
                        ), TradeManager.getNewID(), plugin
                    )
                        .setOnPayListener { success ->
                            if (success) {
                                val event = PlayerTeleportedEvent(
                                    sender,
                                    sender.location,
                                    point.location
                                )
                                Bukkit.getPluginManager().callEvent(event)
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
                    if (!sendGotoRequest(args[1].substring(args[1].indexOf('/') + 1)))
                        sender.error(getter["command.error.playerNotFound"])
                } else if (args[1].startsWith("checkpoint/")) {
                    if (!gotoCheckpoint(info, args[1].substring(args[1].indexOf('/') + 1)))
                        sender.error(getter["user.error.checkpointNotFound"])
                } else {
                    fun doDefault() {
                        if (!sendGotoRequest(args[1]) && !gotoCheckpoint(info, args[1])) {
                            info.friendships.forEach {
                                if (gotoCheckpoint(it.friend, args[1])) {
                                    return
                                }
                            }
                            sender.error(getter["user.error.checkpointOrPlayerNotFound"])
                        }
                    }
                    if (args[1].contains('/')) {
                        val target =
                            try {
                                ServerPlayer.of(args[1].substring(0, args[1].indexOf('/')))
                            } catch (e: Exception) {
                                null
                            }
                        if (target != null) {
                            fun error() = sender.error(getter["user.error.checkpointNotFound"])
                            if (info.friendships[target]?.isFriend != true || !gotoCheckpoint(
                                    target,
                                    args[1].substring(args[1].indexOf('/') + 1)
                                )
                            )
                                error()
                        } else {
                            doDefault()
                        }
                    } else {
                        doDefault()
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
                            sender.error(getter["user.error.moreThanOneInvitations.1"])
                            sender.tip(getter["user.error.moreThanOneInvitations.2"])
                            sender.info(getter["user.error.moreThanOneInvitations.3"])
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
                    val target = Bukkit.getPlayer(args[1])
                    if (target == null) {
                        sender.error(getter["command.playerNotFound"])
                        Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
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
                    plugin.getCommand("user death")?.usage?.let {
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
                                TextUtil.TextColor.GOLD
                            )
                        )

                        val death = info.lastDeath
                        if (death != null) {
                            death.show(sender)
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
                        val death = info.lastDeath
                        if (death == null) {
                            sender.error(getter["user.lastDeath.noRecord"])
                            return true
                        }
                        val prise = Game.env.getInt("backToDeathPrise")
                        sender.info(getter["user.lastDeath.last", death.location.toPrettyString()])
                        sender.tip(getter["user.lastDeath.toGo", Bukkit.getPluginCommand("user bd")!!.usage, prise])
                    }
                }
            }

            "bd" -> {
                val info = PlayerManager.findInfoByPlayer(sender)
                if (info == null) {
                    sender.error(getter["player.unknown"])
                    return true
                }
                val death = info.lastDeath
                if (death == null) {
                    sender.error(getter["user.lastDeath.noRecord"])
                    return true
                }
                val prise = Game.env.getInt("backToDeathPrise")
                if (info.currency < prise) {
                    sender.error(getter["user.error.noSoManyCoins"])
                    return true
                }

                val event = PlayerTeleportedEvent(sender, sender.location, death.location)
                Bukkit.getPluginManager().callEvent(event)
                if (!event.isCancelled) {
                    death.teleport(sender)
                    info.currency -= prise
                    sender.info(getter["user.lastDeath.goTo", prise])
                }

                info.lastDeath = null
            }

            "saveas" -> {
                if (args.size < 2) {
                    sender.error(getter["command.error.usage"])
                    sender.sendUsage("user saveas")
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
                    val point = Checkpoint(sender.location, id)
                    info.checkpoints.add(point)
                    sender.info(getter["user.checkpoint.saved", point.location.toPrettyString(), id])
                    sender.tip(getter["user.checkpoint.toGo", id])
                }
            }

            "delsave" -> {
                if (args.size < 2) {
                    sender.error(getter["command.error.usage"])
                    sender.sendUsage("user delsave")
                    return true
                }
                val info = PlayerManager.findInfoByPlayer(sender)
                if (info == null) {
                    sender.error(getter["player.error.unknown"])
                    return true
                }
                if (!info.removeCheckpoint(args[1])) {
                    sender.error(getter["user.checkpoint.notFound"])
                } else {
                    sender.info(getter["user.checkpoint.del", args[1]])
                }
            }

            "lang" -> {
                if (args.size < 2) {
                    sender.error(getter["command.error.usage"])
                    sender.sendUsage("user lang")
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
                    sender.sendUsage("user transfer")
                    return true
                }

                val info = PlayerManager.findInfoByPlayer(sender)
                if (info == null) {
                    sender.error(getter["player.error.unknown"])
                    return true
                }

                val oldPwd = args[1]
                val pwd = args[2]

                if (!info.matchPassword(pwd)) {
                    sender.error(getter["user.login.failed"])
                    return true
                }

                var result: OfflineInfo? = null
                var errorSent = false
                OfflineInfo.forEach {
                    if (!it.matchPassword(oldPwd) || it.uuid == sender.uniqueId)
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
                    plugin.getCommand("user prefer")?.usage?.let {
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

            "help" -> {
                val content = TextUtil.getCustomizedText(getter["user.helpDoc"])
                sender.sendMessage(*content.split('\n').toTypedArray())
            }

            else -> {
                sender.error(getter["command.error.usage"])
                return false
            }
        }
        return true
    }

    private fun CommandSender.sendUsage(command: String) =
        plugin.getCommand(command)?.usage?.let { sendMessage(it) }
}