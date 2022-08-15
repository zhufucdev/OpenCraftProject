package com.zhufu.opencraft

import com.zhufu.opencraft.Base.tradeWorld
import com.zhufu.opencraft.CurrencySystem.Companion.inventoryMap
import com.zhufu.opencraft.CurrencySystem.Companion.territoryMap
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
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toInfoMessage
import net.citizensnpcs.api.event.NPCLeftClickEvent
import org.bukkit.GameMode
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
import java.io.File

@Suppress("unused")
object EveryThing : Listener {
    val traderInventoryName = TextUtil.getColoredText("服务器商人", TextUtil.TextColor.AQUA, true, false)
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
            if (!getPlayerTerritory(event.player).contains(event.block.location)) {
                event.isCancelled = true
                event.player.error("您不能在此破坏方块")
            } else {
                if (event.block.type != Material.CHEST)
                    return
                val t = SetUpValidateInventory.inventories.firstOrNull { it.baseLocation == event.block.location }
                if (t != null) {
                    event.player.info("已取消物品销售")
                }
            }
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.block.world == tradeWorld && !getPlayerTerritory(event.player).contains(event.blockPlaced.location)) {
            event.isCancelled = true
            event.player.error("您不能在此放置方块")
        }
    }

    /**
     * Player Events
     */
    private fun getPlayerTerritory(player: Player): CurrencySystem.TradeTerritoryInfo {
        var t = territoryMap.firstOrNull { it.player == player.uniqueId }
        if (t == null) {
            territoryMap.add(
                CurrencySystem.TradeTerritoryInfo(
                    player.uniqueId,
                    player.info()?.territoryID.let {
                        if (it != null)
                            it
                        else {
                            player.error(player.getter()["player.error.unknown"])
                            throw IllegalStateException()
                        }
                    }
                ).also {
                    t = it
                }
            )
        }
        return t!!
    }

    @EventHandler
    fun onPlayerTeleported(event: PlayerTeleportedEvent) {
        if (event.to?.world == tradeWorld) {
            val t = getPlayerTerritory(event.player)
            PlayerManager.findInfoByPlayer(event.player)
                ?.apply {
                    if (!isSurveyPassed && remainingDemoTime <= 0) {
                        event.isCancelled = true
                        PlayerManager.showPlayerOutOfDemoTitle(event.player)
                    }
                    status = Info.GameStatus.InLobby
                    inventory.getOrCreate(RESET).load(inventoryOnly = true)
                    if (!isTradeTutorialShown) {
                        CurrencySystem.showTutorial(event.player)
                    }
                    isTerritoryInMessageShown = false
                    isTerritoryOutMessageShown = false
                }
                ?: event.player.error(Language.getDefault("player.error.unknown"))
            event.player.info(getLang(event.player, "trade.territory.info", t.id, t.x, t.z))
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
            val inTerritory =
                getPlayerTerritory(event.player).contains(event.player.location)
            if (!inMsgShown && inTerritory) {
                event.player.sendActionBar(info.getter()["trade.territory.enter"].toInfoMessage())
                info.status = Info.GameStatus.Surviving
                info.inventory.getOrCreate("survivor").load(inventoryOnly = true)
                info.isTerritoryInMessageShown = true
                info.isTerritoryOutMessageShown = false
                if (!event.player.isOp)
                    event.player.gameMode = GameMode.SURVIVAL
            } else if (!outMsgShown && !inTerritory) {
                event.player.sendActionBar(info.getter()["trade.territory.leave"].toInfoMessage())
                info.status = Info.GameStatus.InLobby
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
                VisitorInventory(CurrencySystem.instance, event.player)
            else {
                val si = StatefulSpecialItem[itemInHand]
                if (si is FlyWand) {
                    FlyWandInventory(event.player, si, CurrencySystem.instance)
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
        if (playerInfo.status != Info.GameStatus.Surviving)
            return
        if (event.itemDrop.itemStack.type == Material.WOODEN_AXE) {
            val getter = playerInfo.getter()
            if (!getPlayerTerritory(event.player).contains(event.itemDrop.location)) {
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
        if (event.npc.name == CurrencySystem.npc?.name) {
            val info = PlayerManager.findInfoByPlayer(event.clicker)
            if (info != null && !info.isSurveyPassed && info.npcTradeCount > 10) {
                PlayerManager.onPlayerOutOfDemo(info)
                return
            }
            inventoryMap.add(TraderInventory(event.clicker).apply { show() })
        } else if (event.npc.name == CurrencySystem.npcBack?.name) {
            val info = event.clicker.info()
            if (info == null) {
                event.clicker.error(Language.getDefault("player.error.unknown"))
            } else {
                info.status = Info.GameStatus.Surviving
                info.inventory.getOrCreate("survivor").load()
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
        val inventory = inventoryMap.firstOrNull { it.player.uniqueId == event.whoClicked.uniqueId } ?: return
        event.currentItem ?: return
        event.isCancelled = true
        if (event.slot <= 9 && transMap.containsKey(event.currentItem!!.type)) {
            inventory.select(event.currentItem!!)
        } else {
            when (event.slot) {
                getPositionForLine(2) -> {
                    inventory.subtractOne()
                }

                getPositionForLine(6) -> {
                    inventory.plusOne()
                }

                getPositionForLine(4) -> {
                    if (inventory.selectedItem == null) {
                        event.whoClicked.printTradeError(
                            event.whoClicked.getter()["trade.error.notFound"],
                            TradeManager.getNewID()
                        )
                        return
                    }
                    inventory.confirm()
                }

                else -> inventory.selectSpecialItem(event.currentItem!!)
            }
        }
    }
}