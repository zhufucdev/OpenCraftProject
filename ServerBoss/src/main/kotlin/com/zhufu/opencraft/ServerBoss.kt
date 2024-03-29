package com.zhufu.opencraft

import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.inventory.PaymentDialog
import com.zhufu.opencraft.util.toInfoMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.concurrent.fixedRateTimer

class ServerBoss : JavaPlugin() {
    private var timer: Timer? = null
    private var spawnTime: Long
        get() = config.getLong("spawn", 0)
        set(value) = config.set("spawn", value)

    override fun onEnable() {
        NPCController.init(this)
        ShootingWebHandler.init(this)

        fun setTimer() {
            timer = fixedRateTimer(
                startAt = getNextDate().also {
                    nextDate = it
                    logger.info("Boss will spawn at ${format.format(it)}.")
                },
                period = Duration.ofDays(2).toMillis(),
                action = {
                    NPCController.spawn(++spawnTime)
                    setTimer()
                    this.cancel()
                }
            )
        }
        setTimer()
    }

    override fun onDisable() {
        timer?.cancel()
        saveConfig()
        NPCController.close()
    }

    private fun getNextDate(): Date {
        val minuteOfDay = LocalTime.now(Base.timeZone.toZoneId()).let { it.hour * 60 + it.minute }
        val index = minuteOfDay / spawnPeriod + 1
        return Date.from(
            LocalDate.now(Base.timeZone.toZoneId())
                .atStartOfDay()
                .atZone(Base.timeZone.toZoneId())
                .plusMinutes((index * spawnPeriod).toLong())
                .toInstant()
        )
    }

    private fun Location.spread(): Location {
        val location = clone()
        location.add(Vector.getRandom().setX(0).multiply(10))
        return if (block.isEmpty) {
            val down = location.clone().add(0.0, -1.0, 0.0)
            while (down.block.isEmpty) {
                down.add(0.0, -1.0, 0.0)
            }
            down
        } else {
            val up = location.clone().add(0.0, 1.0, 0.0)
            while (!up.block.isEmpty) {
                up.add(0.0, 1.0, 0.0)
            }
            up
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
                            val dest = NPCController.currentNPC.entity.location.spread()
                            PaymentDialog(
                                player = sender,
                                sellingItems = SellingItemInfo(
                                    item = ItemStack(Material.ENDER_PEARL).updateItemMeta<ItemMeta> {
                                        displayName(getLang(sender, "ui.teleport").toInfoMessage())
                                    },
                                    unitPrise = 3,
                                    amount = 1
                                ),
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
                        if (args.size == 1) {
                            NPCController.spawn(++spawnTime)
                        } else {
                            NPCController.spawn(++spawnTime, EntityType.valueOf(args[1].uppercase()))
                        }
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
        lateinit var nextDate: Date
        val format = SimpleDateFormat("MM/dd HH:mm:ss").apply { timeZone = Base.timeZone }

        /**
         * Period between boss spwans, in minutes
         */
        val spawnPeriod get() = 90.0
    }
}