package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import com.zhufu.opencraft.data.OfflineInfo
import com.zhufu.opencraft.util.toComponent
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.Plugin
import kotlin.collections.ArrayList

class VisitorInventory(plugin: Plugin, val player: Player) : PageInventory<VisitorInventory.VisitorAdapter>(
    "浏览商家".toComponent(),
    VisitorAdapter(),
    36,
    plugin
) {
    class VisitorAdapter : Adapter() {
        val tradeMap = ArrayList<MutableList<TradeInfo>>()

        init {
            TradeManager.forEach {
                val seller = it.seller
                if (seller == null || seller == Base.serverID || it.isDestroyed)
                    return@forEach
                val player = Bukkit.getOfflinePlayer(seller)
                if (player.name != null) {
                    val element = tradeMap.firstOrNull { t -> t.first().seller == player.uniqueId }
                    if (element != null) {
                        element.add(it)
                    } else {
                        tradeMap.add(mutableListOf(it))
                    }
                }
            }
        }

        override val size: Int
            get() = tradeMap.size

        override fun getItem(index: Int, currentPage: Int): ItemStack {
            val element = tradeMap[index]
            return OfflineInfo.findByUUID(element.first().seller!!)!!
                .skullItem
                .updateItemMeta<SkullMeta> {
                    owningPlayer?.name?.let { displayName(Component.text(it)) }
                    lore(element.map {
                        it.item.item.displayName()
                            .append(
                                Component.text(
                                    "×" + it.item.amount
                                            + " -> " + it.item.prise
                                )
                            )
                    })
                }
        }
    }

    init {
        show(player)
        this.setOnItemClickListener { index, item ->
            fun reportError() {
                player.error(getLang(player, "trade.error.territoryNotFound"))
            }
            val sellerID = adapter.tradeMap[index].first().seller
            if (sellerID == null) {
                reportError()
                return@setOnItemClickListener
            }

            val seller = OfflineInfo.findByUUID(sellerID)
            if (seller == null) {
                reportError()
                return@setOnItemClickListener
            }
            val t = TradeTerritoryInfo(seller)
            player.teleport(t.center)
        }
    }
}