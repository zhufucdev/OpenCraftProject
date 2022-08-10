package com.zhufu.opencraft

import com.zhufu.opencraft.inventory.PaymentDialog
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class BlockLocker : JavaPlugin(), Listener {
    override fun onEnable() {
        BlockLockManager.loadFromFile(dataFolder)
        BlockListener.init(this)

        ServerCaller["ChangeChunkOwner"] = {
            val from = it.firstOrNull()?.toString()
            val to = it.getOrNull(1)?.toString()
            if (from != null && to != null)
                BlockLockManager.forEverything { c ->
                    if (c.owner == from) {
                        if (c.name.endsWith("territory")) {
                            BlockLockManager.remove(c)
                        } else {
                            c.owner = to
                        }
                    }
                }
            else throw IllegalStateException("This call must be given two Int parameters.")
        }
        ServerCaller["NewBlock"] = {
            val sender = (it.firstOrNull()
                ?: throw IllegalArgumentException("This call must be given at least one Player parameter.")) as Player
            newBlockFor(sender)
        }
    }

    override fun onDisable() {
        BlockLockManager.saveToFile(dataFolder)
        BlockListener.cleanUp()
    }

    private fun newBlockFor(sender: Player) {
        val getter = sender.getter()

        if (!BlockListener.creationMap.containsKey(sender)) {
            if (sender.location.world == Base.tradeWorld) {
                sender.error(getter["block.error.inTradeWorld"])
                return
            } else if (!sender.isOp && sender.world == Base.lobby) {
                sender.error(getter["block.error.inLobby"])
                return
            }
            sender.tip(getter["block.tip.new"])
            BlockListener.creationMap[sender] = hashMapOf()
        } else {
            val results = BlockListener.creationMap[sender]!!.filterValues { selected -> selected }
            if (results.isEmpty()) {
                sender.error(getter["block.error.selectedNothing"])
                return
            }
            val unitPrise = Game.env.getLong("prisePerBlock")
            PaymentDialog(
                sender,
                SellingItemInfo(ItemStack(Material.CHEST), unitPrise, results.size),
                TradeManager.getNewID(),
                this
            )
                .setOnPayListener { success ->
                    if (success) {
                        val newName: String
                        run {
                            var max = 0
                            val prefix = getter["command.unnamed"]
                            BlockLockManager.forEach {
                                if (it.name.startsWith(prefix)) {
                                    val order = it.name.substring(prefix.length).toIntOrNull() ?: return@forEach
                                    if (order > max) max = order
                                }
                            }
                            newName = "$prefix${max + 1}"
                        }

                        val uuid = sender.uniqueId.toString()
                        BlockLockManager.add(
                            if (results.size == 1) {
                                BlockLockManager.BlockInfo(results.keys.first(), newName)
                            } else {
                                BlockLockManager.GroupBlockInfo(newName).apply {
                                    owner = uuid
                                    results.keys.forEachIndexed { index, location ->
                                        add(
                                            BlockLockManager.BlockInfo(location, "$newName-$index").apply {
                                                owner = uuid
                                            }
                                        )
                                    }
                                }
                            }.apply {
                                owner = uuid
                                BlockLockManager.selected[sender] = this
                                sender.success(
                                    getter[
                                            "block.success",
                                            getter["block.${if (this is BlockLockManager.BlockInfo) "block" else "group"}"],
                                            newName,
                                            sender.name
                                    ]
                                )
                            }
                        )
                        sender.tip(getter["ui.block.selecting.tip.2"])
                        BlockListener.cleanFor(sender)
                    } else {
                        sender.error(getter["trade.error.poor"])
                    }
                    true
                }
                .setOnCancelListener {
                    sender.info(getter["block.cancel"])
                    BlockListener.cleanFor(sender)
                }
                .show()
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "bl") {
            val getter = sender.getter()
            fun groupOrBlock(info: BlockLockManager.BaseInfo, get: Language.LangGetter? = null): String {
                return if (info is BlockLockManager.GroupBlockInfo) {
                    get?.get("block.group")
                        ?: getter["block.group"]
                } else {
                    get?.get("block.block")
                        ?: getter["block.block"]
                }
            }
            if (args.isEmpty()) {
                if (sender !is Player) {
                    sender.error(getter["command.error.playerOnly"])
                    return true
                }
                newBlockFor(sender)
            } else {
                val selected = sender is Player && BlockLockManager.selected.containsKey(sender)
                when (args.first()) {
                    "set" -> {
                        (if (selected) 2 else 3).apply {
                            if (args.size < this) {
                                sender.error(getter["command.error.tooFewArgs", this])
                                return true
                            }
                        }
                        val player = Bukkit.getOfflinePlayer(args[1])
                        val name = if (selected) BlockLockManager.selected[sender]!!.name else args[2]

                        val info = if (selected) BlockLockManager.selected[sender] else BlockLockManager[name]
                        if (info == null) {
                            sender.error(getter["block.error.groupNotFound"])
                            return true
                        }
                        if (sender !is Player || sender.isOp || info.owner == sender.uniqueId.toString()) {
                            info.accessible.add(player.uniqueId)
                            sender.success(getter["block.accessAdded", player.name, name, groupOrBlock(info)])
                            player.player?.apply {
                                success(getter["block.addedAccess", info.name, groupOrBlock(info)])
                            }
                        } else {
                            sender.error(getter["block.error.notOwnBlock"])
                        }
                    }

                    "unset" -> {
                        (if (selected) 2 else 3).apply {
                            if (args.size < this) {
                                sender.error(getter["command.error.tooFewArgs", this])
                                return true
                            }
                        }
                        val player = Bukkit.getOfflinePlayer(args[1])
                        val name = if (selected) BlockLockManager.selected[sender]!!.name else args[2]

                        if (name != "clear") {
                            val info = if (selected) BlockLockManager.selected[sender] else BlockLockManager[name]
                            if (info == null) {
                                sender.error(getter["block.error.groupNotFound"])
                                return true
                            }
                            if (sender !is Player || info.owner == sender.uniqueId.toString()) {
                                info.accessible.remove(player.uniqueId)
                                sender.success(getter["block.accessCleared", player.name, info.name])
                                player.player?.apply {
                                    info(
                                        getLang(this, "block.clearedAccess", info.name, groupOrBlock(info))
                                    )
                                }
                            }
                        } else {
                            val removed = StringBuilder()
                            BlockLockManager.forEverything {
                                if ((sender !is Player || it.owner == sender.uniqueId.toString() || sender.isOp) && it.accessible.remove(
                                        player.uniqueId
                                    )
                                ) removed.append("${it.name} ")
                            }
                            val replacement = if (removed.isNotEmpty()) removed.toString() else "Empty"
                            sender.success(getter["block.accessEmptyed", player.name, replacement])
                            player.player?.apply {
                                info(
                                    getLang(this, "block.emptyedAccess", replacement)
                                )
                            }
                        }
                    }

                    "del" -> {
                        (if (selected) 1 else 2).apply {
                            if (args.size < this) {
                                sender.error(getter["command.error.tooFewArgs", this])
                                return true
                            }
                        }
                        val info = if (selected) BlockLockManager.selected[sender] else BlockLockManager[args[1]]
                        if (info == null) {
                            sender.error(getter["block.error.groupNotFound"])
                            return false
                        }
                        if (sender !is Player || sender.isOp || sender.uniqueId.toString() == info.owner) {
                            val name = info.name
                            BlockLockManager.remove(info)
                            sender.success(getter["block.delete", name, groupOrBlock(info)])
                            Bukkit.getPlayer(UUID.fromString(info.owner))?.apply {
                                info(
                                    getLang(this, "block.deleted", sender.name, info.name, groupOrBlock(info))
                                )
                            }
                            BlockLockManager.remove(name)
                        } else {
                            sender.error(getter["block.error.notOwnBlock"])
                        }
                    }

                    "check" -> {
                        (if (selected) 1 else 2).apply {
                            if (args.size < this) {
                                sender.error(getter["command.error.tooFewArgs", this])
                                return true
                            }
                        }
                        val info = if (selected) BlockLockManager.selected[sender] else BlockLockManager[args[1]]
                        if (info == null) {
                            sender.error(getter["block.error.groupNotFound"])
                            return true
                        }
                        if (sender !is Player || sender.isOp || sender.uniqueId.toString() == info.owner) {
                            val map = info.accessMap.toSortedMap()
                            if (map.isEmpty()) {
                                sender.error(getter["block.statics.empty"])
                            } else {
                                sender.sendMessage(
                                    TextUtil.getColoredText(
                                        getter["block.statics.title", info.name],
                                        TextUtil.TextColor.GOLD,
                                        false,
                                        underlined = false
                                    )
                                )
                                val playerMap = HashMap<String, Int>()
                                val format = SimpleDateFormat("yyyy/MM/dd/hh:mm:ss aa")
                                val groupOrNot = groupOrBlock(info)
                                map.forEach {
                                    val player = Bukkit.getOfflinePlayer(it.value).name ?: it.value.toString()
                                    sender.sendMessage(getter["block.statics.piece", player, format.format(it.key), groupOrNot])
                                    playerMap[player] = playerMap.getOrDefault(player, 0) + 1
                                }
                                val conclusionBuilder = StringBuilder(getter["block.statics.among"])
                                playerMap.forEach { t, u ->
                                    conclusionBuilder.append(getter["block.statics.conclusion", t, u])
                                }
                                conclusionBuilder.deleteCharAt(conclusionBuilder.lastIndex)
                                sender.sendMessage(conclusionBuilder.toString())
                            }
                        } else {
                            sender.error(getter["block.error.notOwnBlock"])
                        }
                    }

                    "rename" -> {
                        (if (selected) 2 else 3).apply {
                            if (args.size < this) {
                                sender.error(getter["command.error.tooFewArgs", this])
                                return true
                            }
                        }
                        val info = if (selected) BlockLockManager.selected[sender] else BlockLockManager[args[2]]
                        if (info == null) {
                            sender.error(getter["block.error.groupNotFound"])
                            return true
                        }

                        val newName = args.last()
                        val oldName = info.name
                        info.name = newName
                        sender.success(getter["block.rename.done", oldName, newName])
                    }

                    else -> {
                        when {
                            BlockLockManager.contains(args.first()) -> {
                                // Grouping
                                val blocks = ArrayList<BlockLockManager.BlockInfo>()
                                val warn = ArrayList<IllegalArgumentException>()
                                var group: BlockLockManager.GroupBlockInfo? = null
                                var groupName = ""
                                var mode = 'U'
                                for (i in 0 until args.size) {
                                    val name = args[i]
                                    if (mode == 'U') {
                                        when (name) {
                                            "->" -> mode = 'G'
                                            "<-" -> mode = 'D'
                                            else -> {
                                                val info = BlockLockManager[name]
                                                if (info is BlockLockManager.BlockInfo)
                                                    blocks.add(info)
                                                else {
                                                    val cause =
                                                        getter["block.grouping.reason", BlockLockManager.BlockInfo::class.simpleName, if (info == null) "null" else info::class.simpleName]
                                                    warn.add(
                                                        IllegalArgumentException(
                                                            getter["block.grouping.giveUp", name],
                                                            Throwable(cause)
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        groupName = name
                                        val info = BlockLockManager[name]
                                        if (info is BlockLockManager.GroupBlockInfo) {
                                            group = info
                                        }
                                        break
                                    }
                                }
                                if (mode == 'U') {
                                    sender.error(getter["block.grouping.modeUnknown"])
                                    sender.sendMessage(
                                        getter[
                                                "block.grouping.or",
                                                Bukkit.getPluginCommand("bl ->")!!.usage,
                                                Bukkit.getPluginCommand("bl <-")!!.usage
                                        ]
                                    )
                                    return true
                                }
                                if (group == null) {
                                    if (groupName.isNotEmpty()) {
                                        sender.info(getter["block.grouping.createAs", groupName])
                                        val new = BlockLockManager.GroupBlockInfo(groupName)
                                        new.owner =
                                            (sender as? Player)?.uniqueId?.toString() ?: getter["command.error.unknown"]
                                        group = new
                                        BlockLockManager.add(new)
                                    } else {
                                        sender.sendMessage(
                                            TextUtil.error(getter["block.grouping.nameUnknown"]),
                                            Bukkit.getPluginCommand("bl ${if (mode == 'G') "->" else "<-"}")!!.usage
                                        )
                                        return true
                                    }
                                }
                                warn.forEach {
                                    sender.sendMessage("${it.message}，${getter["block.grouping.because", it.cause?.message]}")
                                }
                                if (mode == 'G') {
                                    blocks.forEach { block ->
                                        if (!group.contains(block)) {
                                            BlockLockManager.remove(block)
                                            group.add(block)
                                        } else {
                                            sender.warn(getter["block.grouping.alreadyExist", block.name])
                                        }
                                    }
                                } else {
                                    blocks.forEach { block ->
                                        if (group.contains(block)) {
                                            group.remove(block)
                                            BlockLockManager.add(block)
                                        } else {
                                            sender.warn(getter["block.grouping.notExist", block.name])
                                        }
                                    }
                                }
                                sender.success(getter["block.grouping.done"])
                            }

                            else -> {
                                sender.error(getter["command.error.usage"])
                                return false
                            }
                        }
                    }
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
        val players = ArrayList<String>()
        server.offlinePlayers.forEach { players.add(it.name!!) }

        val result = mutableListOf<String>()
        fun addPointerResult(): MutableList<String> {
            if (sender !is Player) {
                return mutableListOf()
            }
            val block = sender.getTargetBlock(null, 5)
            return when {
                args.size <= 4 -> if (args.size % 2 == 0) {
                    mutableListOf(block.z.toString())
                } else {
                    mutableListOf(block.x.toString())
                }

                args.size == 6 -> players.toMutableList()
                else -> mutableListOf()
            }
        }

        fun addGroupResult(): MutableList<String> {
            if (args.first().isNotEmpty() && !BlockLockManager.containsBlock(args.first())) {
                return mutableListOf()
            }
            val r = ArrayList<String>()
            if (!args.contains("->") && !args.contains("<-")) {
                BlockLockManager.forEachBlock {
                    if ((sender is Player && it.owner == sender.uniqueId.toString()) || (it.owner == "op" && sender.isOp)) {
                        r.add(it.name)
                    }
                }
                if (args.size > 1) {
                    r.add("->")
                    r.add("<-")
                }
            } else {
                BlockLockManager.forEach {
                    if (it is BlockLockManager.GroupBlockInfo && (sender is Player && it.owner == sender.uniqueId.toString()) || (it.owner == "op" && sender.isOp)) {
                        r.add(it.name)
                    }
                }
            }
            return if (args.last().isEmpty()) {
                r
            } else {
                r.filter { it.startsWith(args.last()) }.toMutableList()
            }
        }

        fun addCommandResults(): MutableList<String> {
            val commands = listOf("set", "unset", "del", "check", "formatLore")
            if (args.isEmpty() || args.size == 1 && commands.any { it.startsWith(args.first()) }) {
                val r = ArrayList<String>()
                commands.forEach { if (it.startsWith(args.first())) r.add(it) }
                return r.toMutableList()
            } else {
                when (args.first()) {
                    "set" -> {
                        when (args.size) {
                            1 -> return players.toMutableList()
                            2 -> {
                                return if (args[1].isNotEmpty()) {
                                    val r = ArrayList<String>()
                                    players.forEach { if (it.startsWith(args[1])) r.add(it) }
                                    r.toMutableList()
                                } else {
                                    players.toMutableList()
                                }
                            }

                            3 -> {
                                return if (args[2].isNotEmpty()) {
                                    val r = ArrayList<String>()
                                    BlockLockManager.forEverything { if (it.fullPath.startsWith(args[2])) r.add(it.fullPath) }
                                    r.toMutableList()
                                } else {
                                    val r = ArrayList<String>()
                                    BlockLockManager.forEverything { r.add(it.fullPath) }
                                    r
                                }
                            }
                        }
                    }

                    "unset" -> {
                        when (args.size) {
                            1 -> return players.toMutableList()
                            2 -> {
                                return if (args[1].isNotEmpty()) {
                                    val r = ArrayList<String>()
                                    players.forEach { if (it.startsWith(args[1])) r.add(it) }
                                    r.toMutableList()
                                } else {
                                    players.toMutableList()
                                }
                            }

                            3 -> {
                                return if (args[2].isNotEmpty()) {
                                    val r = ArrayList<String>()
                                    r.add("clear")
                                    BlockLockManager.forEverything { if (it.fullPath.startsWith(args[2])) r.add(it.fullPath) }
                                    r
                                } else {
                                    val r = ArrayList<String>()
                                    BlockLockManager.forEverything { r.add(it.fullPath) }
                                    r
                                }
                            }
                        }
                    }

                    "del" -> {
                        val r = mutableListOf<String>()
                        BlockLockManager.forEach {
                            if (sender is Player && (it.owner == sender.uniqueId.toString() || sender.isOp) || sender is ConsoleCommandSender) {
                                r.add(it.name)
                            }
                        }
                        return when (args.size) {
                            1 -> {
                                r
                            }

                            2 -> {
                                r.filter { it.startsWith(args[1]) }.toMutableList()
                            }

                            else -> mutableListOf()
                        }
                    }

                    "check" -> {
                        val r = mutableListOf<String>()
                        BlockLockManager.forEach {
                            if (sender is Player && (it.owner == sender.uniqueId.toString() || sender.isOp) || sender is ConsoleCommandSender) {
                                r.add(it.name)
                            }
                        }
                        return when (args.size) {
                            1 -> {
                                r
                            }

                            2 -> {
                                r.filter { it.startsWith(args[1]) }.toMutableList()
                            }

                            else -> mutableListOf()
                        }
                    }
                }
            }
            return mutableListOf()
        }
        if (args.isEmpty() || (0 until if (args.size <= 4) args.size else 3).all { it -> args[it].all { it == '-' || it.isDigit() } }) {
            result.addAll(addPointerResult())
        }
        result.addAll(addGroupResult())
        result.addAll(addCommandResults())
        return result
    }
}