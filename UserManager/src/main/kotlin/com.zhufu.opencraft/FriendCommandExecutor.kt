package com.zhufu.opencraft

import com.zhufu.opencraft.data.ServerPlayer
import com.zhufu.opencraft.inventory.PaymentDialog
import com.zhufu.opencraft.player_community.Friendship
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.special_item.Coin
import com.zhufu.opencraft.util.toComponent
import com.zhufu.opencraft.util.toInfoMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class FriendCommandExecutor(private val plugin: UserManager) : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val getter = sender.getter()
        if (sender is Player) {
            val info = sender.info()
            if (info == null) {
                sender.error(getter["player.error.unknown"])
            } else {
                fun checkIsFriend(target: ServerPlayer): Friendship? {
                    if (info.friendships.contains(target)) {
                        val friend = info.friendships[target]!!
                        if (friend.isFriend) {
                            return friend
                        } else {
                            sender.error(getter["user.friend.error.notAccepted", target.name])
                        }
                    } else {
                        sender.error(getter["user.friend.error.noSuchFriend", target.name])
                    }
                    return null
                }
                if (info.isLogin) {
                    when (args.size) {
                        0 -> {
                            sender.info(getter["user.friend.list"])
                            if (info.friendships.count() > 0)
                                info.friendships.forEach {
                                    sender.sendMessage(
                                        Component.text(it.friend.name ?: getter["command.error.unknown"]).toBuilder()
                                            .apply { b ->
                                                if (it.sharedInventory != null)
                                                    b.append(
                                                        Component.text(';')
                                                            .append(
                                                                getter["user.friend.sharingInventory"].toComponent()
                                                                    .color(NamedTextColor.GREEN)
                                                                    .decorate(TextDecoration.BOLD)
                                                            )
                                                    )
                                                if (it.sharedCheckpoints.isNotEmpty())
                                                    b.append(
                                                        Component.text(';')
                                                            .append(getter["user.friend.sharingCheckpoints", it.sharedCheckpoints.size]
                                                                .toComponent()
                                                                .color(NamedTextColor.GREEN)
                                                                .decorate(TextDecoration.BOLD))
                                                    )
                                            }
                                    )
                                }
                            else
                                sender.info(getter["user.friend.empty"])
                        }

                        1 -> {
                            val friend = info.friendships[args.first()]
                            if (friend != null) {
                                if (friend.isFriend)
                                    friend.printStatics(sender, getter)
                                else
                                    sender.error(getter["user.friend.error.notAccepted", args.first()])
                            } else {
                                sender.error(getter["user.friend.error.noSuchFriend", args.first()])
                            }
                        }

                        2 -> {
                            val name = args.first()
                            val target = try {
                                ServerPlayer.of(name = name).takeIf { it != info }
                            } catch (e: Exception) {
                                null
                            }
                            if (target == null) {
                                sender.error(getter["player.error.notFound", name])
                                return true
                            }
                            when (args[1]) {
                                "add" -> {
                                    fun sendPassMessage(r: Friendship) {
                                        sender.success(getter["user.friend.added", name])
                                        r.friend.messagePool.apply {
                                            add(
                                                text = "\${user.friend.added,${sender.name}}",
                                                type = MessagePool.Type.System
                                            ).let {
                                                it.recordTime()
                                                if (target.isOnline)
                                                    it.sendTo(target.onlinePlayerInfo!!)
                                            }
                                        }
                                    }
                                    if (!info.friendships.contains(target)) {
                                        val r = info.friendships.add(target)
                                        if (r.isFriend) {
                                            sendPassMessage(r)
                                        } else {
                                            sender.success(getter["user.friend.sent"])
                                            target.messagePool.apply {
                                                val a = add(
                                                    text = "\$info\${user.friend.request.title,${sender.name}}",
                                                    type = MessagePool.Type.OneTime
                                                )
                                                val b = add(
                                                    text = "\$tip\${user.friend.request.tip,${sender.name}," +
                                                            "${sender.name}}",
                                                    type = MessagePool.Type.OneTime
                                                )
                                                a.recordTime()
                                                if (target.isOnline) {
                                                    with(target.onlinePlayerInfo!!) {
                                                        sendTo(this, a)
                                                        sendTo(this, b)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val friendInfo = info.friendships[target]!!
                                        when {
                                            friendInfo.isFriend -> sender.error(getter["user.friend.added", name])
                                            friendInfo.isParentAdder -> sender.error(getter["user.friend.sent"])
                                            else -> {// Pass the request
                                                friendInfo.startAt = System.currentTimeMillis()
                                                sendPassMessage(friendInfo)
                                            }
                                        }
                                    }
                                }

                                "del" -> {
                                    if (info.friendships.contains(target)) {
                                        if (info.friendships.remove(target)) {
                                            sender.success(getter["user.friend.removed", name])
                                            target.messagePool.add(
                                                text = "\$info\${user.friend.wasRemoved,${sender.name}}",
                                                type = MessagePool.Type.System
                                            ).let {
                                                it.recordTime()
                                                if (target.isOnline)
                                                    it.sendTo(target.onlinePlayerInfo!!)
                                            }
                                        } else {
                                            sender.error(getter["user.friend.error.notRemoved", name])
                                        }
                                    } else {
                                        sender.error(getter["user.friend.error.noSuchFriend", name])
                                    }
                                }

                                "inventory" -> {
                                    val friendInfo = checkIsFriend(target)
                                    if (friendInfo != null) {
                                        if (friendInfo.sharedInventory == null) {
                                            PaymentDialog(
                                                player = sender,
                                                sellingItems = SellingItemInfo(
                                                    item = ItemStack(Material.CHEST).updateItemMeta<ItemMeta> {
                                                        displayName(getter["user.friend.sharingInventory"].toInfoMessage())
                                                    },
                                                    amount = 1,
                                                    unitPrise = 10
                                                ),
                                                plugin = plugin
                                            )
                                                .setOnPayListener { success ->
                                                    if (success) {
                                                        friendInfo.createSharedInventory()
                                                        sender.success(getter["user.friend.inventory.created"])
                                                        sender.openInventory(friendInfo.sharedInventory!!)
                                                    } else {
                                                        sender.error(getter["trade.error.poor"])
                                                    }
                                                    true
                                                }
                                                .setOnConfirmListener {
                                                    sender.info(getter["user.friend.inventory.cancelled"])
                                                }
                                                .show()
                                        } else {
                                            sender.openInventory(friendInfo.sharedInventory!!)
                                        }
                                    }
                                }

                                else -> {
                                    sender.error(getter["command.error.usage"])
                                    sender.sendUsage("friend")
                                }
                            }
                        }

                        else -> {
                            val name = args.first()
                            val target = try {
                                ServerPlayer.of(name = name)
                            } catch (e: Exception) {
                                null
                            }
                            if (target == null) {
                                sender.error(getter["player.error.notFound", name])
                                return true
                            }
                            val friendInfo = checkIsFriend(target)
                            if (friendInfo != null) {
                                when (args[1]) {
                                    "transfer" -> {
                                        val t = args[2].toBigIntegerOrNull()
                                        if (t == null) {
                                            sender.error(getter["command.error.argNonDigit"])
                                        } else {
                                            if (t + target.currency.toBigInteger() > Long.MAX_VALUE.toBigInteger()) {
                                                sender.error(getter["user.friend.transfer.error.outOfBound", args[2]])
                                                return true
                                            }
                                            val amount = t.toLong()
                                            if (amount < 0) {
                                                sender.error(getter["user.friend.transfer.error.minus"])
                                                return true
                                            }

                                            PaymentDialog(
                                                player = sender,
                                                sellingItems = SellingItemInfo(
                                                    item = Coin(getter),
                                                    amount = 1,
                                                    unitPrise = amount
                                                ),
                                                plugin = plugin
                                            )
                                                .setOnPayListener { success ->
                                                    if (success) {
                                                        target.currency += amount
                                                        friendInfo.transferred += amount

                                                        sender.success(getter["user.friend.transfer.out", amount, name])
                                                        target.messagePool.add(
                                                            text = "\$success\${user.friend.transfer.in,${sender.name},$amount}",
                                                            type = MessagePool.Type.System
                                                        ).let {
                                                            it.recordTime()
                                                            if (target.isOnline)
                                                                it.sendTo(target.onlinePlayerInfo!!)
                                                        }
                                                    } else {
                                                        sender.error(getter["trade.error.poor"])
                                                    }
                                                    true
                                                }
                                                .setOnCancelListener {
                                                    sender.info(getter["user.friend.transfer.cancelle"])
                                                }
                                                .show()
                                        }
                                    }

                                    "msg" -> {
                                        val message = buildString {
                                            for (i in 2 until args.size) {
                                                append(args[i])
                                                append(' ')
                                            }
                                        }
                                        if (message.isBlank()) {
                                            sender.error(getter["user.friend.error.blankMessage"])
                                        } else {
                                            target.messagePool.add(
                                                text = message,
                                                type = MessagePool.Type.Friend
                                            ).apply {
                                                this.sender = info.uuid
                                                recordTime()
                                                if (target.isOnline)
                                                    sendTo(target.onlinePlayerInfo!!)
                                            }

                                            sender.success(getter["user.friend.messaged", name])
                                        }
                                    }

                                    "share" -> {
                                        val path = args[2]
                                        val share = if (args.size < 4) true else args[3] != "off"
                                        when {
                                            path == "location" -> {
                                                friendInfo.shareLocation = share
                                                if (share) sender.success(getter["user.friend.location.start", name])
                                                else sender.info(getter["user.friend.location.stop", name])
                                            }

                                            path.startsWith("checkpoint/") -> {
                                                val n = path.substring(11)
                                                val point = info.checkpoints.firstOrNull { it.name == n }
                                                if (point != null) {
                                                    if (share) {
                                                        if (!friendInfo.sharedCheckpoints.contains(point)) {
                                                            friendInfo.shareCheckpoint(point)
                                                        }
                                                        sender.success(getter["user.friend.point.start", n, name])
                                                    } else {
                                                        if (friendInfo.sharedCheckpoints.contains(point)) {
                                                            friendInfo.removeSharedCheckpoint(point)
                                                        }
                                                        sender.info(getter["user.friend.point.stop", n, name])
                                                    }
                                                } else {
                                                    sender.error(getter["user.checkpoint.notFound"])
                                                }
                                            }

                                            else -> sender.error(getter["user.friend.error.object", path])
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    sender.error(getter["user.error.notLoginYet"])
                }
            }
        } else {
            sender.error(getter["command.error.playerOnly"])
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (sender is Player) {
            val info = sender.info()
            if (info != null) {
                if (args.size == 1) {
                    val names = mutableListOf<String>()
                    info.friendships.forEach {
                        names.add(it.name ?: return@forEach)
                    }
                    ServerPlayer.forEachSaved {
                        val n = it.name
                        if (n != null && names.contains(n)) {
                            names.add(n)
                        }
                    }
                    return if (args.first().isEmpty()) {
                        names
                    } else {
                        names.filter { it.startsWith(args.first()) }.toMutableList()
                    }
                } else if (args.size == 2) {
                    val operations = mutableListOf("add", "del", "transfer", "msg", "inventory", "share")
                    return if (args[1].isEmpty()) {
                        operations
                    } else {
                        operations.filter { it.startsWith(args[1]) }.toMutableList()
                    }
                } else if (args[1] == "share") {
                    if (args.size == 3) {
                        val options = mutableListOf("location")
                        info.checkpoints.forEach {
                            options.add("checkpoint/${it.name}")
                        }
                        return if (args[2].isEmpty()) {
                            options
                        } else {
                            options.filter { it.startsWith(args[2]) }.toMutableList()
                        }
                    } else if (args.size == 4) {
                        val options = mutableListOf("on", "off")
                        return if (args[3].isEmpty()) {
                            options
                        } else {
                            options.filter { it.startsWith(args[3]) }.toMutableList()
                        }
                    }
                }
            }
        }
        return mutableListOf()
    }

    private fun CommandSender.sendUsage(command: String) =
        plugin.getCommand(command)?.usage?.let { sendMessage(it) }
}