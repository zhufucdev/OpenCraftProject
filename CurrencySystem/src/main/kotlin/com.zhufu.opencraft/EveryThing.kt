package com.zhufu.opencraft

import com.zhufu.opencraft.TradeManager.loadTradeCompass
import com.zhufu.opencraft.Base.tradeWorld
import com.zhufu.opencraft.BlockLockManager.toXZ
import com.zhufu.opencraft.CurrencySystem.Companion.inventoryMap
import com.zhufu.opencraft.CurrencySystem.Companion.territoryMap
import com.zhufu.opencraft.CurrencySystem.Companion.transMap
import com.zhufu.opencraft.DualInventory.Companion.RESET
import com.zhufu.opencraft.inventory.TraderInventory.Companion.getPositionForLine
import com.zhufu.opencraft.inventory.FlyWandInventory
import com.zhufu.opencraft.inventory.TraderInventory
import com.zhufu.opencraft.inventory.SetUpValidateInventory
import com.zhufu.opencraft.inventory.VisitorInventory
import com.zhufu.opencraft.TradeManager.printTradeError
import com.zhufu.opencraft.special_items.FlyWand
import com.zhufu.opencraft.special_items.SpecialItem
import net.citizensnpcs.api.event.NPCRightClickEvent
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.*

object EveryThing : Listener,PluginBase {
    val traderInventoryName = TextUtil.getColoredText("服务器商人", TextUtil.TextColor.AQUA, true, false)

    /**
     * Reload Event
     */
    @EventHandler
    fun onServerReload(event: ServerReloadEvent){
        TradeManager.saveToFile(File(CurrencySystem.tradeRoot,"tradeInfos.json"))
    }

    /**
     * Block Events
     */
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (event.block.world == tradeWorld) {
            if (BlockLockManager[event.block.location]?.canAccess(event.player) != true) {
                event.isCancelled = true
                event.player.sendMessage(TextUtil.error("您不能在此破坏方块"))
            }else {
                if (event.block.type != Material.CHEST)
                    return
                val t = SetUpValidateInventory.inventories.firstOrNull { it.baseLocation == event.block.location }
                if (t != null) {
                    event.player.sendTitle(TextUtil.info("已取消物品销售"),"",7,30,7)
                }
            }
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent){
        if (event.block.world == tradeWorld && BlockLockManager[event.block.location]?.canAccess(event.player) != true){
            event.isCancelled = true
            event.player.sendMessage(TextUtil.error("您不能在此放置方块"))
        }
    }

    /**
     * Player Events
     */
    private fun getPlayerTerritory(player: UUID): CurrencySystem.TradeTerritoryInfo{
        var t = territoryMap.firstOrNull { it.player == player }
        if (t == null) {
            territoryMap.add(
                    CurrencySystem.TradeTerritoryInfo(
                            player,
                            (territoryMap.maxBy { it.id }?.id ?: -1) + 1
                    )
                            .also { info ->
                                info.location = info.getChunkLocation()
                                t = info
                            }
            )
        }
        return t!!
    }
    @EventHandler
    fun onPlayerTeleported(event: com.zhufu.opencraft.events.PlayerTeleportedEvent) {
        if (event.to.world == tradeWorld) {
            val t = getPlayerTerritory(event.player.uniqueId)
            PlayerManager.findInfoByPlayer(event.player)
                    ?.also {
                        if (!it.isSurveyPassed && it.remainingDemoTime <= 0){
                            event.isCancelled = true
                            PlayerManager.showPlayerOutOfDemoTitle(event.player)
                        }

                        if (!it.tag.isSet("territoryID"))
                            it.tag.set("territoryID", t.id)
                        if (!it.tag.isSet("territoryLocation"))
                            it.tag.set("territoryLocation", t.location)
                        it.status = Info.GameStatus.InLobby
                        it.inventory.create(RESET).load(inventoryOnly = true)
                        if (!it.tag.getBoolean("isTradeTutorialShown",false)){
                            CurrencySystem.showTutorial(event.player)
                        }
                        it.saveTag()
                        it.tag.set("isTerritoryInMessageShown",false)
                        it.tag.set("isTerritoryOutMessageShown",false)
                    }
                    ?:event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
            event.player.sendMessage(TextUtil.info("您的领地(ID:${t.id})位于区块(${t.location.x},${t.location.z})"))
            if (!BlockLockManager.contains("${t.player}_territory")) {
                BlockLockManager.add(
                        t.toBlockLocker(t.player.toString())
                )
            }
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent){
        if (event.to!!.world == tradeWorld){
            val player = PlayerManager.findInfoByPlayer(event.player)
            if (player == null){
                event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
                return
            }
            if (player.status == Info.GameStatus.InTutorial){
                return
            }
            val inMsgShown = player.tag.getBoolean("isTerritoryInMessageShown",false)
            val outMsgShown = player.tag.getBoolean("isTerritoryOutMessageShown",false)
            val inTerritory = BlockLockManager["${event.player.uniqueId}_territory"]?.contains(event.to!!.toXZ(),event.to!!.toXZ()) == true
            if (!inMsgShown && inTerritory) {
                event.player.sendActionText(TextUtil.info("您已进入自己的领地"))
                player.status = Info.GameStatus.Surviving
                player.inventory.create("survivor").load(inventoryOnly = true)
                player.tag.set("isTerritoryInMessageShown",true)
                player.tag.set("isTerritoryOutMessageShown",false)
                if (!event.player.isOp)
                    event.player.gameMode = GameMode.SURVIVAL
            } else if (!outMsgShown && !inTerritory) {
                event.player.sendActionText(TextUtil.info("您已退出自己的领地"))
                player.status = Info.GameStatus.InLobby
                loadTradeCompass(player)
                player.tag.set("isTerritoryOutMessageShown",true)
                player.tag.set("isTerritoryInMessageShown",false)
                if (!event.player.isOp)
                    event.player.gameMode = GameMode.ADVENTURE
            }
        }
    }

    @EventHandler
    fun onPlayerRightClick(event: PlayerInteractEvent){
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK){
            val itemInHand = event.player.inventory.itemInMainHand
            if (event.player.world == tradeWorld && itemInHand.type == Material.COMPASS)
                VisitorInventory(CurrencySystem.mInstance,event.player)
            else if (SpecialItem.isSpecial(itemInHand)){
                if (FlyWand.isThis(itemInHand)){
                    FlyWandInventory(event.player,CurrencySystem.mInstance)
                }
            }
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent){
        val chunkInfo = BlockLockManager[event.player.location]
        val playerInfo = PlayerManager.findInfoByPlayer(event.player)
        if (playerInfo == null){
            event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
            return
        }
        if (playerInfo.status != Info.GameStatus.Surviving)
            return
        if (event.itemDrop.itemStack.type == Material.WOODEN_AXE){
            if (chunkInfo?.owner != event.player.uniqueId.toString()){
                event.player.sendMessage(TextUtil.error("抱歉，但您不能在此处创建商店"))
                return
            }
            if (BuilderListener.isInBuilderMode(event.player)){
                event.player.sendMessage(TextUtil.error("抱歉，但您不能在此时创建商店"))
                return
            }
            val l = event.itemDrop.location
            val itemsAround = ArrayList<Item>()
            l.world!!.entities.forEach {
                if (it is Item && it.location.distance(l) <= 2){
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
            if (itemsAround.isNotEmpty()){
                if (!isTypeTheSame){
                    event.player.sendMessage(TextUtil.error("抱歉，但你不能同时出售不同种类的物品"))
                    return
                }
                val itemSell = itemsAround.first().itemStack
                if (transMap.containsKey(itemSell.type)){
                    event.player.sendMessage(TextUtil.error("抱歉，但您不能出售服务器已有的物品"))
                    return
                }
                val amount = itemsAround.sumBy { it.itemStack.amount }
                itemSell.amount = amount
                println("${event.player.name} tries to sell ${itemsAround.first().itemStack.type.name}*$amount")
                val location = itemsAround.first().location
                if (location.block.type != Material.AIR){
                    event.player.sendMessage(TextUtil.error("抱歉，但您的商店不能覆盖已有方块"))
                    return
                }
                SetUpValidateInventory(location,itemSell,event.player)
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
    fun onNPCClick(event: NPCRightClickEvent) {
        if (event.npc == CurrencySystem.npc) {
            val info = PlayerManager.findInfoByPlayer(event.clicker)
            if (info != null && !info.isSurveyPassed && info.tag.getInt("npcTrade",0) > 10){
                PlayerManager.onPlayerOutOfDemo(info)
                return
            }
            inventoryMap.add(TraderInventory(event.clicker).apply { show() })
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (inventoryMap.any { it.player == event.player }) {
            val info = PlayerManager.findInfoByPlayer(event.player.uniqueId)
            if (info == null){
                event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
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
        val inventory = inventoryMap.firstOrNull{ it.player.uniqueId == event.whoClicked.uniqueId } ?: return
        event.currentItem?:return
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
                        event.whoClicked.printTradeError("找不到所选物品", TradeManager.getNewID())
                        return
                    }
                    inventory.confirm()
                }
                else -> inventory.selectSpecialItem(event.currentItem!!)
            }
        }
    }
}