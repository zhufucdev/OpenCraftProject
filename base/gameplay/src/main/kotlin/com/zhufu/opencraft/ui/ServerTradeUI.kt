package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*

import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class ServerTradeUI(private val owner: Info, private val selector: GoodSelectionUI, plugin: Plugin) :
    DraggableInventory(plugin, 1, owner.getter()["rpg.ui.trade.title"].toInfoMessage()) {
    val getter = owner.getter()
    private var mSelection: ItemStack? = null
    private var mPrise: Prise<*>? = null

    /**
     * Select the prise of this [selection] preferred by the trader.
     * Set to GeneralPrise(-1) to clear.
     */
    var prise: Prise<*>
        get() = if (mPrise == null) if (selection.isNullOrEmpty()) 0.toGP() else Prise.evaluate(selection!!) else mPrise!!
        set(value) {
            mPrise = if (value is GeneralPrise && value.value == -1L) null else value
            setItem(4, put)
        }
    var selection: ItemStack?
        get() = mSelection?.takeIf { it.type != Material.AIR }
        set(value) {
            if (!value.isNullOrEmpty())
                setItem(1, value, false)
            else
                setItem(1, ItemStack(Material.BARRIER)
                    .updateItemMeta<ItemMeta> { setDisplayName(getter["rpg.ui.trade.select.none"].toInfoMessage()) })
            mSelection = value
        }

    private val glass = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE).updateItemMeta<ItemMeta> {
        setDisplayName(getter["rpg.void"])
    }

    private val lack
        get() = ItemStack(Material.BARRIER)
            .updateItemMeta<ItemMeta> { setDisplayName(getter["rpg.ui.trade.lack"].toErrorMessage()) }
    private val put
        get() = ItemStack(Material.EMERALD).updateItemMeta<ItemMeta> {
            setDisplayName(getter["rpg.ui.trade.put.name"].toTipMessage() + Language.getDefault("rpg.ui.upgrade.place.right"))
            lore = TextUtil.formatLore(getter["trade.pay.currencyConsume", prise.toString(getter)])
        }

    init {
        val select = ItemStack(Material.NETHER_STAR).updateItemMeta<ItemMeta> {
            setDisplayName(getter["rpg.ui.trade.select.name"].toInfoMessage())
        }

        setItem(0, select)
        setItem(2, select)
        setItem(4, put)
        listOf(3, 5, 6, 8).forEach { setItem(it, glass) }
    }

    override fun onClick(event: InventoryClickEvent) {
        val player = showing ?: return
        if (event.slot == 0 || event.slot == 2) {
            selector.setSelectListener { itemStack, prise ->
                selection = itemStack
                this.prise = prise
            }
            selector.setCloseListener { runSync { show(player) } }
            selector.show(player)
            event.isCancelled = true
        }
    }

    fun canExchange(out: ArrayList<ItemStack>? = null): Boolean {
        val currency = inventory.getItem(7) ?: return false
        val player = owner.player
        val unit = Prise.of(currency.clone().apply { amount = 1 })
        val exchange = (unit * (prise / unit + if((prise % unit).isZero()) 0 else 1) - prise).generateItem(player)
        out?.addAll(exchange)
        return exchange.size <= 1
    }

    override fun onPlace(event: InventoryClickEvent) {
        if (event.slot != 7) return
        runSync {
            val item = inventory.getItem(7) ?: return@runSync
            val selection = mSelection ?: return@runSync
            if (Prise.of(item, showing) >= prise) {
                if (canExchange())
                    setItem(1, selection, false)
                else
                // Unable to exchange is when the exchange takes more than one slot
                    setItem(
                        1,
                        ItemStack(Material.BARRIER)
                            .updateItemMeta<ItemMeta> { setDisplayName(getter["rpg.ui.trade.unableToExchange"].toErrorMessage()) }
                    )
            } else {
                setItem(1, lack)
            }
        }
    }

    override fun onTake(event: InventoryClickEvent) {
        when (event.slot) {
            1 -> {
                val currency = inventory.getItem(7)
                val player = showing
                val exchange = arrayListOf<ItemStack>()
                if (currency == null || Prise.of(currency) < prise || !canExchange(exchange) || player == null) {
                    event.isCancelled = true
                    return
                }
                // Cost money
                val unit = Prise.of(currency.clone().apply { amount = 1 })
                setItem(
                    7,
                    currency.clone().apply { amount -= prise / unit + if ((prise % unit).isZero()) 0 else 1 },
                    protected = false,
                    overridable = true
                )
                // Place exchange
                if (exchange.isNotEmpty()) {
                    setItem(6, exchange.first(), false)
                } else {
                    setItem(6, glass)
                }
            }
            6 -> {
                runSync {
                    setItem(6, glass)
                }
            }
            7 -> {
                val currency = inventory.getItem(7)
                if (currency == null || Prise.of(currency) < prise) {
                    setItem(1, lack)
                }
            }
        }
    }

    override fun onClose() {
        (6..7).forEach {
            inventory.getItem(it).takeIf { item -> it != 6 || item != glass }
                ?.let { item ->
                    owner.player.setInventory(item, item.amount)
                    clear(it)
                }
        }
        setItem(7, ItemStack(Material.AIR), protected = false, overridable = true)
    }
}