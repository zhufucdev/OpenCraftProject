package com.zhufu.opencraft

import com.mongodb.client.model.Filters
import com.zhufu.opencraft.data.Database
import com.zhufu.opencraft.data.DualInventory
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

object TradeManager {
    /**
     * Check if the total prise reaches the limit of int
     * @return true if reaches the limit
     */
    fun checkLimit(itemInfo: SellingItemInfo): Boolean {
        val long = itemInfo.unitPrise * itemInfo.amount.toLong()
        return long > Int.MAX_VALUE
    }

    fun checkLimit(unitPrise: Long, amount: Int): Boolean {
        val long = unitPrise * amount
        return long > Int.MAX_VALUE
    }

    fun loadTradeCompass(player: Info) {
        if (player.player.world == Base.tradeWorld && !TradeTerritoryInfo(player).contains(player.player.location)) {
            player.inventory.getOrCreate(DualInventory.RESET).load(inventoryOnly = true)
            player.player.inventory.addItem(
                ItemStack(Material.COMPASS)
                    .updateItemMeta<ItemMeta> {
                        displayName(
                            Language[player, "trade.explorer"]
                                .toComponent()
                                .color(NamedTextColor.AQUA)
                        )
                    }
            )
        }
    }

    lateinit var plugin: JavaPlugin

    fun init(plugin: JavaPlugin) {
        TradeManager.plugin = plugin
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            forEach {
                it.summonNPC()
            }
        }, 20)
    }

    fun forEach(l: (TradeInfo) -> Unit) = Database.trades.find().forEach {
        l(TradeInfo.of(it))
    }

    operator fun get(player: HumanEntity): List<TradeInfo> =
        Database.trades.find(Filters.eq("seller", player.uniqueId)).map { TradeInfo.of(it) }.toList()

    fun trade(
        seller: UUID,
        what: ItemStack,
        amount: Int,
        unitPrise: Long,
        buyer: UUID? = null,
        location: Location? = null,
        ignoreInventoryItem: Boolean = false
    ): TradeInfo {
        if (seller != Base.serverID) {
            val sellerPlayer = Bukkit.getPlayer(seller)
            if (sellerPlayer != null) {
                val detailedItem = what.displayName()
                    .style(TextUtil.INFO_STYLE)
                    .append(Component.text("×$amount").style(TextUtil.INFO_STYLE))
                    .hoverEvent { HoverEvent.showItem(what.type.key, what.amount) as HoverEvent<Any> }

                sellerPlayer.sendMessage(
                    Component.text("您正在以${unitPrise}货币出售")
                        .append(detailedItem)
                        .style(TextUtil.INFO_STYLE)
                )
                Bukkit.getOnlinePlayers().filter { it.uniqueId != seller }.forEach {
                    it.sendMessage(
                        Component.text("${sellerPlayer.name}正在以${unitPrise}货币出售")
                            .append(detailedItem)
                            .style(TextUtil.INFO_STYLE)
                    )
                }
            }
        }
        return TradeInfo(SellingItemInfo(item = what, unitPrise, amount)).apply {
            this.location = location
            setSeller(seller, ignoreInventoryItem, buyer != null)
            setBuyer(buyer, item.amount)
            add(this)
        }
    }

    private fun add(tradeInfo: TradeInfo) {
        Database.trades.insertOne(tradeInfo.toDocument())
    }

    enum class TradeResult {
        SUCCESSFUL, FAILED, UPDATE
    }

    fun buy(who: HumanEntity, info: TradeInfo, howMany: Int): TradeResult {
        val amount = info.item.amount

        val r = info.setBuyer(who.uniqueId, howMany, cost = true)
        return if (!r)
            TradeResult.FAILED
        else {
            if (amount == howMany) {
                info.destroy()
                TradeResult.SUCCESSFUL
            } else TradeResult.UPDATE
        }
    }

    fun remove(info: TradeInfo) {
        Database.trades.findOneAndDelete(Filters.eq("_id", info.id))
    }
}