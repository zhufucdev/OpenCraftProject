package com.zhufu.opencraft

import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.inventory.PaymentDialog
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.floor

class ServerBoss : JavaPlugin() {
    private var timer: Timer? = null
    private var spawnTime: Long
        get() = config.getLong("spawn", 0)
        set(value) = config.set("spawn", value)

    override fun onEnable() {
        NPCController.init(this)

        fun setTimer() {
            timer = fixedRateTimer(
                startAt = getNextDate().also {
                    nextDate = it
                    logger.info("Boss will spawn at ${format.format(it)}.")
                },
                period = 10,
                action = {
                    Bukkit.getScheduler().callSyncMethod(this@ServerBoss) {
                        NPCController.spawn(++spawnTime)
                        setTimer()
                    }
                    this.cancel()
                }
            )
        }
        fixSpawnCount()
        setTimer()
    }

    override fun onDisable() {
        timer?.cancel()
        saveConfig()
        NPCController.close()
    }

    private var spawnCount = 0
    private fun fixSpawnCount() {
        val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        spawnCount = (hourOfDay / spawnPeriod).toInt() + 1
        logger.info("Fixed spawn count is $spawnCount.")
    }

    private fun getNextDate(): Date {
        return with(Calendar.getInstance()) {
            if (spawnCount >= 24 / spawnPeriod) {
                set(Calendar.DAY_OF_MONTH, get(Calendar.DAY_OF_MONTH) + 1)
                spawnCount = 0
            }
            val targetHour = spawnCount * spawnPeriod
            set(Calendar.HOUR_OF_DAY, floor(targetHour).toInt())
            set(Calendar.MINUTE, (60 * (targetHour - floor(targetHour))).toInt())
            set(Calendar.SECOND, 0)

            spawnCount++

            Date(timeInMillis)
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val getter = sender.getter()
        if (command.name == "boss") {
            if (args.isEmpty()) {
                if (sender is Player) {
                    if (NPCController.isCurrentBossAlive) {
                        val info = sender.info()
                        if (info?.isLogin == true) {
                            val dest = NPCController.currentNPC.entity.location
                            PaymentDialog(
                                player = sender,
                                sellingItems = SellingItemInfo(
                                    item = ItemStack(Material.ENDER_PEARL).updateItemMeta<ItemMeta> {
                                        displayName(getLang(sender, "ui.teleport").toInfoMessage())
                                    },
                                    unitPrise = 3,
                                    amount = 1
                                ),
                                id = TradeManager.getNewID(),
                                plugin = this
                            )
                                .setOnPayListener { success ->
                                    if (success) {
                                        val event = PlayerTeleportedEvent(
                                            sender,
                                            sender.location,
                                            dest
                                        )
                                        Bukkit.getPluginManager().callEvent(event)
                                        if (!event.isCancelled) {
                                            sender.teleport(dest)
                                            sender.success(getter["boss.teleport"])
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
                        } else
                            sender.error(getter["user.error.notLoginYet"])
                    } else
                        sender.error(getter["boss.error.notSpawned"])
                } else {
                    sender.error(getter["command.error.playerOnly"])
                }
            } else {
                when (args.first()) {
                    "spawn" -> {
                        if (!sender.isOp) {
                            sender.error(getter["command.error.permission"])
                            return true
                        }
                        NPCController.spawn(++spawnTime)
                    }
                    "difficulty" -> {
                        if (args.size == 1) {
                            sender.info(getter["boss.difficulty", spawnTime])
                        } else {
                            if (!sender.isOp) {
                                sender.error(getter["command.error.permission"])
                                return true
                            }
                            val num = args[1].toLongOrNull()
                            if (num == null) {
                                sender.error("command.error.argNotDigit")
                            } else {
                                spawnTime = num
                                sender.success(getter["boss.difChanged", num])
                            }
                        }
                    }
                    else -> {
                        sender.error(getter["command.error.usage"])
                        return false
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
        if (command.name == "boss") {
            if (args.size == 1) {
                val commands = mutableListOf("difficulty")
                if (sender.isOp)
                    commands.add("spawn")
                return if (args.first().isEmpty())
                    commands
                else {
                    commands.filter { it.startsWith(args.first()) }.toMutableList()
                }
            }
        }
        return mutableListOf()
    }

    companion object {
        var nextDate: Date? = null
        val format = SimpleDateFormat("MM/dd HH:mm:ss")
        val spawnPeriod get() = 1.5
    }
}