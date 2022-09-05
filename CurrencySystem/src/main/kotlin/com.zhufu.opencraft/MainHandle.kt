package com.zhufu.opencraft

import com.zhufu.opencraft.Base.TutorialUtil.gmd
import com.zhufu.opencraft.Base.TutorialUtil.linearMotion
import com.zhufu.opencraft.Base.TutorialUtil.tplock
import com.zhufu.opencraft.Base.tradeWorld
import com.zhufu.opencraft.CurrencySystem.Companion.transMap
import com.zhufu.opencraft.TradeManager.loadTradeCompass
import com.zhufu.opencraft.TradeManager.plugin
import com.zhufu.opencraft.TradeManager.printTradeError
import com.zhufu.opencraft.data.DualInventory.Companion.RESET
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.inventory.FlyWandInventory
import com.zhufu.opencraft.inventory.SetUpValidateInventory
import com.zhufu.opencraft.inventory.TraderInventory
import com.zhufu.opencraft.inventory.TraderInventory.Companion.getPositionForLine
import com.zhufu.opencraft.inventory.VisitorInventory
import com.zhufu.opencraft.special_item.FlyWand
import com.zhufu.opencraft.special_item.StatefulSpecialItem
import com.zhufu.opencraft.util.*
import net.citizensnpcs.api.event.NPCLeftClickEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import java.io.File
import java.time.Duration

@Suppress("unused")
object MainHandle : Listener {
    private lateinit var plugin: Plugin
    fun init(plugin: Plugin) {
        this.plugin = plugin
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    val traderInventoryName =
        TextUtil.getColoredText("服务器商人", TextUtil.TextColor.AQUA, bold = true, underlined = false)
    val backNPCName = "生存模式".toInfoMessage()

    /**
     * Reload Event
     */
    @EventHandler
    fun onServerReload(event: ServerReloadEvent) {
        TradeManager.saveToFile(File(CurrencySystem.tradeRoot, "tradeInfos.json"))
    }

    /**
     * Block Events
     */
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (event.block.world == tradeWorld) {
            val info = event.player.info()
            val getter = info.getter()
            if (info == null) {
                event.player.error(getter["player.error.unknown"])
                event.isCancelled = true
                return
            }
            if (!TradeTerritoryInfo(info).contains(event.block.location)) {
                event.isCancelled = true
                event.player.error(getter["trade.error.outOfTerritory"])
            } else {
                if (event.block.type != Material.CHEST)
                    return
                val t = SetUpValidateInventory.inventories.firstOrNull { it.baseLocation == event.block.location }
                if (t != null) {
                    event.player.info(getter["trade.sellCancelled"])
                }
            }
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val info = event.player.info()
        val getter = info.getter()
        if (info == null) {
            event.player.error(getter["player.error.unknown"])
            event.isCancelled = true
            return
        }
        if (event.block.world == tradeWorld && !TradeTerritoryInfo(info).contains(event.blockPlaced.location)) {
            event.isCancelled = true
            event.player.error(getter["trade.error.outOfTerritory"])
        }
    }

    @EventHandler
    fun onPlayerTeleported(event: PlayerTeleportedEvent) {
        if (event.to?.world == tradeWorld) {
            val info = PlayerManager.findInfoByPlayer(event.player)
            val getter = info.getter()
            if (info == null) {
                event.player.error(getter["player.error.unknown"])
                event.isCancelled = true
                return
            }
            val t = TradeTerritoryInfo(info)

            info.apply {
                if (!isSurveyPassed && remainingDemoTime <= 0) {
                    event.isCancelled = true
                    PlayerManager.showPlayerOutOfDemoTitle(event.player)
                }
                status = Info.GameStatus.Surviving
                inventory.getOrCreate(RESET).load(inventoryOnly = true)
                if (!isTradeTutorialShown) {
                    showTutorial(event.player)
                }
                isTerritoryInMessageShown = false
                isTerritoryOutMessageShown = false
            }
            event.player.info(getter["trade.territory.info", info.territoryID, t.x, t.z])
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (event.to.world == tradeWorld) {
            val info = PlayerManager.findInfoByPlayer(event.player)
            if (info == null) {
                event.player.error(Language.getDefault("player.error.unknown"))
                return
            }
            if (info.status == Info.GameStatus.InTutorial) {
                return
            }
            val inMsgShown = info.isTerritoryInMessageShown
            val outMsgShown = info.isTerritoryOutMessageShown
            val inTerritory = TradeTerritoryInfo(info).contains(event.player.location)
            if (!inMsgShown && inTerritory) {
                event.player.sendActionBar(info.getter()["trade.territory.enter"].toInfoMessage())
                info.inventory.getOrCreate("survivor").load(inventoryOnly = true)
                info.isTerritoryInMessageShown = true
                info.isTerritoryOutMessageShown = false
                if (!event.player.isOp)
                    event.player.gameMode = GameMode.SURVIVAL
            } else if (!outMsgShown && !inTerritory) {
                event.player.sendActionBar(info.getter()["trade.territory.leave"].toInfoMessage())
                loadTradeCompass(info)
                info.isTerritoryOutMessageShown = true
                info.isTerritoryInMessageShown = false
                if (!event.player.isOp)
                    event.player.gameMode = GameMode.ADVENTURE
            }
        }
    }

    @EventHandler
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            val itemInHand = event.player.inventory.itemInMainHand
            if (event.player.world == tradeWorld && itemInHand.type == Material.COMPASS)
                VisitorInventory(plugin, event.player)
            else {
                val si = StatefulSpecialItem[itemInHand]
                if (si is FlyWand) {
                    FlyWandInventory(event.player, si, plugin)
                }
            }
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val playerInfo = event.player.info()
        if (playerInfo == null) {
            event.player.error(Language.getDefault("player.error.unknown"))
            return
        }
        if (playerInfo.status != Info.GameStatus.Surviving) {
            return
        }
        if (event.itemDrop.itemStack.type == Material.WOODEN_AXE) {
            val getter = playerInfo.getter()
            if (event.player.world == tradeWorld
                && !TradeTerritoryInfo(playerInfo).contains(event.itemDrop.location)
            ) {
                event.player.error(getter["trade.error.store.wrongPlace"])
                return
            }
            val l = event.itemDrop.location
            val itemsAround = ArrayList<Item>()
            l.world!!.entities.forEach {
                if (it is Item) {
                    val distance = it.location.distance(l)
                    if (distance <= 5)
                        itemsAround.add(it)
                }
            }
            var isTypeTheSame = true
            if (itemsAround.isNotEmpty()) {
                val type = itemsAround.first().itemStack.type
                for (i in 1 until itemsAround.size) {
                    if (itemsAround[i].itemStack.type != type) {
                        isTypeTheSame = false
                        break
                    }
                }
            }
            if (itemsAround.isNotEmpty()) {
                if (!isTypeTheSame) {
                    event.player.error(getter["trade.error.store.mixed"])
                    return
                }
                val itemSell = itemsAround.first().itemStack
                if (transMap.containsKey(itemSell.type)) {
                    event.player.error(getter["trade.error.store.duplicated"])
                    return
                }
                val amount = itemsAround.sumOf { it.itemStack.amount }
                itemSell.amount = amount
                plugin.logger.info("${event.player.name} tries to sell ${itemsAround.first().itemStack.type.name}*$amount")
                val location = itemsAround.first().location
                if (location.block.type != Material.AIR) {
                    event.player.error(getter["trade.error.store.replacing"])
                    return
                }
                SetUpValidateInventory(location.apply { yaw = event.player.location.yaw - 180 }, itemSell, event.player)
                //Clean
                itemsAround.forEach { it.remove() }
                event.player.inventory.addItem(ItemStack(Material.WOODEN_AXE))
                event.itemDrop.remove()
            }
        }
    }

    /**
     * NPC Events
     */
    @EventHandler
    fun onNPCClick(event: NPCLeftClickEvent) {
        if (event.npc == CurrencySystem.npc) {
            val info = PlayerManager.findInfoByPlayer(event.clicker)
            if (info != null && !info.isSurveyPassed && info.npcTradeCount > 10) {
                PlayerManager.onPlayerOutOfDemo(info)
                return
            }
            inventoryMap.add(TraderInventory(event.clicker, plugin).apply { show() })
        } else if (event.npc == CurrencySystem.npcBack) {
            val info = event.clicker.info()
            if (info == null) {
                event.clicker.error(Language.getDefault("player.error.unknown"))
            } else {
                val inv = info.inventory.getOrCreate("survivor")
                val call = PlayerTeleportedEvent(event.clicker, event.clicker.location, inv.location)
                Bukkit.getPluginManager().callEvent(call)
                if (!call.isCancelled) {
                    info.status = Info.GameStatus.Surviving
                    inv.load()
                }
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (inventoryMap.any { it.player == event.player }) {
            val info = PlayerManager.findInfoByPlayer(event.player.uniqueId)
            if (info == null) {
                event.player.error(Language.getDefault("player.error.unknown"))
                return
            }
            loadTradeCompass(info)
            inventoryMap.removeIf { it.player == event.player }
        }
    }

    @EventHandler
    fun onInventoryClickItem(event: InventoryClickEvent) {
        if (event.whoClicked.location.world != tradeWorld)
            return
        val trader = inventoryMap.firstOrNull { it.player.uniqueId == event.whoClicked.uniqueId } ?: return
        if (event.inventory != trader.inventory) {
            return
        }
        event.currentItem ?: return
        event.isCancelled = true
        if (event.slot <= 9 && transMap.containsKey(event.currentItem!!.type)) {
            trader.select(event.currentItem!!)
        } else {
            when (event.slot) {
                getPositionForLine(2) -> {
                    trader.subtractOne()
                }

                getPositionForLine(6) -> {
                    trader.plusOne()
                }

                getPositionForLine(4) -> {
                    if (trader.selectedItem == null) {
                        event.whoClicked.printTradeError(
                            event.whoClicked.getter()["trade.error.notFound"],
                            TradeManager.getNewID()
                        )
                        return
                    }
                    trader.confirm()
                }

                else -> trader.selectSpecialItem(event.currentItem!!)
            }
        }
    }
}

val inventoryMap = arrayListOf<TraderInventory>()

private fun showTitle(
    player: Player,
    getter: Language.LangGetter,
    titleCode: String,
    subtitleCode: String,
    showTime: Int,
    instant: Boolean = false
) {
    val title =
        Title.title(
            getter[titleCode].toErrorMessage(),
            Component.text(getter[subtitleCode], NamedTextColor.AQUA),
            if (!instant)
                Title.Times.times(
                    Duration.ofMillis(250),
                    Duration.ofSeconds(5),
                    Duration.ofMillis(250)
                )
            else
                Title.Times.times(
                    Duration.ZERO,
                    Duration.ofSeconds(5),
                    Duration.ZERO
                )
        )
    player.showTitle(title)
    Thread.sleep(showTime * 1000L + 50)
}

fun showTutorial(player: Player) {
    Bukkit.getScheduler().runTaskAsynchronously(Base.pluginCore, Runnable {
        val info = PlayerManager.findInfoByPlayer(player)
        if (info == null) {
            player.error(Language.getDefault("player.error.unknown"))
            return@Runnable
        }
        info.status = Info.GameStatus.InTutorial
        val getter = Language[info]
        player.gmd(GameMode.SPECTATOR)
        player.tplock(
            tradeWorld.spawnLocation
                .add(Vector(0, 30, 0))
                .setDirection(Vector(0, -90, 0)),
            5 * 20
        )
        showTitle(
            player,
            getter,
            "trade.tutorial.1.title",
            "trade.tutorial.1.subtitle",
            5
        )

        val l1 = Location(tradeWorld, 7.5, 62.0, 6.5)
            .setDirection(Vector(0, 0, -90))
        player.tplock(l1, 7 * 20)
        showTitle(
            player,
            getter,
            "trade.tutorial.2.title",
            "trade.tutorial.2.subtitle",
            7
        )


        val l2 = TradeTerritoryInfo(info)
        val center = l2.center
        val location2 =
            Location(tradeWorld, center.x.toDouble(), tradeWorld.spawnLocation.y + 30, center.z.toDouble())
                .setDirection(Vector(0, -90, 0))
        player.tplock(location2, 7 * 20)
        showTitle(
            player,
            getter,
            "trade.tutorial.3.title",
            "trade.tutorial.3.subtitle",
            3
        )
        showTitle(
            player,
            getter,
            "trade.tutorial.3.title",
            "trade.tutorial.4.subtitle",
            4,
            true
        )
        val l3Top = tradeWorld.spawnLocation.clone()
            .add(Vector(0, 30, 0))
            .setDirection(Vector(0, -90, 0))
        val l3Bottom = l3Top.clone().add(Vector(0.0, -15.0, 0.0))
        val scheduler = Bukkit.getScheduler()
        scheduler.runTask(plugin) { _ ->
            player.teleport(l3Top)
        }
        player.linearMotion(l3Bottom, 13 * 20)
        showTitle(
            player,
            getter,
            "trade.tutorial.5.title",
            "trade.tutorial.5.subtitle",
            4
        )

        showTitle(
            player,
            getter,
            "trade.tutorial.5.title",
            "trade.tutorial.6.subtitle",
            4,
            true
        )
        showTitle(
            player,
            getter,
            "trade.tutorial.5.title",
            "trade.tutorial.7.subtitle",
            6,
            true
        )

        player.linearMotion(tradeWorld.spawnLocation.setDirection(Vector(1, 0, 0)), 75)
        Thread.sleep(4000L)

        player.gmd(GameMode.ADVENTURE)
        player.showTitle(
            Title.title(
                getter["tutorial.begin"].toSuccessMessage(),
                Component.text(""),
                Title.Times.times(
                    Duration.ofMillis(150),
                    Duration.ofSeconds(2),
                    Duration.ofSeconds(1)
                )
            )
        )


        PlayerManager.findInfoByPlayer(player)
            ?.also {
                it.status = Info.GameStatus.Surviving
                it.isTradeTutorialShown = true
            }
            ?: player.sendMessage(Language.getDefault("player.error.unknown").toWarnMessage())
    })
}