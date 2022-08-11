package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import com.zhufu.opencraft.Base.tradeWorld
import com.zhufu.opencraft.data.OfflineInfo
import com.zhufu.opencraft.util.toComponent
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.Plugin
import java.util.*
import kotlin.collections.ArrayList

class VisitorInventory(plugin: Plugin, val player: Player) : PageInventory<VisitorInventory.VisitorAdapter>(
    "浏览商家[uuid:$id]".toComponent(),
    VisitorAdapter(),
    54,
    plugin
) {
    class VisitorAdapter : PageInventory.Adapter() {
        val tradeMap = ArrayList<MutableList<TradeInfo>>()

        init {
            TradeManager.forEach {
                if (it.getSeller() == "server")
                    return@forEach
                val player = Bukkit.getOfflinePlayer(UUID.fromString(it.getSeller()))
                if (player.name != null) {
                    val element = tradeMap.firstOrNull { t -> t.first().getSeller() == player.uniqueId.toString() }
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
            return OfflineInfo.findByUUID(UUID.fromString(element.first().getSeller()))!!
                .skullItem
                .updateItemMeta<ItemMeta> {
                    lore(element.map {
                        it.items!!.item.displayName()
                            .append(
                                Component.text(
                                    "×" + it.items!!.amount
                                            + "->" + it.items!!.prise
                                )
                            )
                    })
                }
        }
    }

    init {
        show(player)
        this.setOnItemClickListener { index, item ->
            val t = CurrencySystem.territoryMap.firstOrNull {
                it.player.toString() == adapter.tradeMap[index].first().getSeller()
            }
            if (t == null) {
                player.error("抱歉，但我们无法找到该玩家的领地")
                return@setOnItemClickListener
            }
            player.teleport(t.center)
        }
    }
}