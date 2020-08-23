package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class ServerTradeUI(owner: Info, plugin: Plugin) :
    DraggableInventory(plugin, 1, owner.getter()["rpg.ui.trade.title"].toInfoMessage()) {
    val getter = owner.getter()
    private var mSelection: ItemStack? = null
    private var mPrise: Prise<*>? = null

    /**
     * Select the prise of this [selection] preferred by the trader.
     * Set to GeneralPrise(-1) to clear.
     */
    var prise: Prise<*>
        get() = if (mPrise == null) if (selection.isNullOrEmpty()) 0.toGP() else Prise.of(selection!!) else mPrise!!
        set(value) {
            mPrise = if (value is GeneralPrise && value.value == -1L) null else value
        }
    var selection: ItemStack?
        get() = mSelection?.takeIf { it.type != Material.AIR }
        set(value) {
            if (!value.isNullOrEmpty())
                setItem(1, value!!)
            else
                setItem(1, ItemStack(Material.BARRIER)
                    .updateItemMeta<ItemMeta> { setDisplayName(getter["trade.trader.select.none"].toInfoMessage()) })
            mSelection = value
        }

    private val glass = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE).updateItemMeta<ItemMeta> {
        setDisplayName(getter["trade.trader.placeholder"])
    }
    init {
        val select = ItemStack(Material.NETHER_STAR).updateItemMeta<ItemMeta> {
            setDisplayName(getter["trade.trader.select.name"].toInfoMessage())
        }
        val put = ItemStack(Material.EMERALD).updateItemMeta<ItemMeta> {
            setDisplayName(getter["trade.trader.put.name"].toTipMessage())
            lore = TextUtil.formatLore(getter["trade.trader.pay.currencyConsume", prise.toString(getter)])
        }
        setItem(0, select)
        setItem(2, select)
        setItem(4, put)
        listOf(3, 5, 6, 8).forEach { setItem(it, glass) }
    }

    override fun onPlace(event: InventoryClickEvent) {
        if (event.slot != 7) return
        
    }
}