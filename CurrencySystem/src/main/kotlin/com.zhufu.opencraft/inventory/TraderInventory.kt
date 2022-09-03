package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import com.zhufu.opencraft.CurrencySystem.Companion.transMap
import com.zhufu.opencraft.special_item.FlyWand
import com.zhufu.opencraft.special_item.Insurance
import com.zhufu.opencraft.special_item.Portal
import com.zhufu.opencraft.special_item.StatefulSpecialItem
import com.zhufu.opencraft.util.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class TraderInventory(val player: Player, private val plugin: Plugin) {
    private val getter = player.getter()
    private val donate: ItemStack = ItemStack(Material.GOLDEN_APPLE).updateItemMeta<ItemMeta> {
        displayName("捐赠".toSuccessMessage())
        lore(
            listOf(
                "为了表示感谢，我们会给予您一定数量的货币".toComponent(),
                "货币数量=在线时长(分钟)*捐赠金额*3".toInfoMessage()
            )
        )
    }
    private val flyWandIcon = FlyWand(getter).froze()
    private val portalIcon = Portal(getter).froze()
    private val insuranceIcon = Insurance(getter, player.name).froze()
    val inventory = Bukkit.createInventory(null, 36, MainHandle.traderInventoryName)
        .apply {
            setItem(size - 9, flyWandIcon)
            setItem(size - 8, portalIcon)
            setItem(size - 7, insuranceIcon)
            setItem(size - 6, donate)
        }
    lateinit var modeSwitcher: ItemStack
    var mode: Short = 0

    companion object {
        fun getPositionForLine(line: Int) = 9 * 2 + line
    }

    init {
        setMode()
    }

    fun show() {
        val info = PlayerManager.findInfoByPlayer(player)
        if (info == null) {
            player.sendMessage(Language.getDefault("player.error.unknown").toErrorMessage())
            return
        }
        info.inventory.getOrCreate("survivor").load(false, true)
        player.openInventory(inventory)
    }

    var selectedItem: ItemStack? = null
    private fun setMode() {
        fun getText(num: Int) = "汇率为${if (mode == 0.toShort()) "1:$num" else "$num:1"}".toInfoMessage()

        var i = (9 - transMap.size) / 2f.roundToInt()
        transMap.entries.forEach {
            if (it.value != -1L) {
                inventory.setItem(
                    i,
                    ItemStack(it.key, if (it.value <= 64) it.value.toInt() else 1)
                        .also { item ->
                            val meta = item.itemMeta
                            meta!!.lore(listOf(getText(it.value.toInt())))
                            item.itemMeta = meta
                        }
                )
                i++
            }
        }
        inventory.setItem(
            inventory.size - 1,
            if (mode == 0.toShort()) {
                ItemStack(Material.DROPPER).updateItemMeta<ItemMeta> {
                    displayName("兑换模式".toInfoMessage())
                    lore(listOf("从背包兑换矿石成货币".toInfoMessage(), "点击切换".toTipMessage()))
                }
            } else {
                ItemStack(Material.CHEST).updateItemMeta<ItemMeta> {
                    displayName("购买模式".toInfoMessage())
                    lore(listOf("用货币购买矿石到背包".toInfoMessage(), "点击切换".toTipMessage()))
                }
            }.also { modeSwitcher = it }
        )
    }

    fun select(current: ItemStack) {
        amount = 1
        selectedItem = current.clone()
        inventory.setItem(getPositionForLine(2),
            ItemStack(Material.NETHER_STAR)
                .updateItemMeta<ItemMeta> {
                    displayName("少${if (mode == 0.toShort()) "兑换" else "购买"}一个".toInfoMessage())
                }
        )
        inventory.setItem(getPositionForLine(6),
            ItemStack(Material.NETHER_STAR)
                .updateItemMeta<ItemMeta> {
                    displayName("多${if (mode == 0.toShort()) "兑换" else "购买"}一个".toInfoMessage())
                }
        )
        inventory.setItem(
            getPositionForLine(4),
            selectedItem!!
                .updateItemMeta<ItemMeta> {
                    amount = 1
                    displayName("点击确认".toTipMessage())
                }
        )
    }

    fun selectSpecialItem(current: ItemStack) {
        when (current.displayName()) {
            flyWandIcon.displayName() -> {
                val price = FlyWand.MAX_TIME_REMAINING * FlyWand.PRICE_PER_MIN / 60
                PaymentDialog(
                    player,
                    SellingItemInfo(FlyWand(getter).froze(), price.roundToLong(), 1),
                    TradeManager.getNewID(),
                    plugin
                ).setOnPayListener { success ->
                    if (success) {
                        val info = player.info()!!

                        val survivor = info.inventory.getOrCreate("survivor")
                        if (survivor.any { item -> StatefulSpecialItem[item] is FlyWand }) {
                            player.error(getter["wand.duplicate.title"])
                            player.tip(getter["wand.duplicate.tip"])
                            return@setOnPayListener false
                        } else if (!survivor.addItem(FlyWand(getter))) {
                            player.error(getter["trade.error.inventoryFull"])
                            return@setOnPayListener false
                        }
                        true
                    } else {
                        player.error(getter["trade.error.poor"])
                        true
                    }
                }
                    .setOnCancelListener {
                        player.info(getter["trade.cancelled"])
                    }
                    .show()
            }

            portalIcon.displayName() -> {
                PaymentDialog(
                    player,
                    SellingItemInfo(Portal(getter), Portal.PRICE.toLong(), 1),
                    TradeManager.getNewID(),
                    plugin
                ).setOnPayListener { success ->
                    val info = player.info()!!
                    if (success) {
                        val survivor = info.inventory.getOrCreate("survivor")
                        if (!survivor.addItem(Portal(getter))) {
                            player.error(getter["trade.error.inventoryFull"])
                            return@setOnPayListener false
                        }
                    } else {
                        player.error(getter["trade.error.poor"])
                    }
                    true
                }
                    .setOnCancelListener {
                        player.info(getter["trade.cancelled"])
                    }
                    .show()
            }

            insuranceIcon.displayName() -> {
                val insurance = Insurance(getter, player.name)
                PaymentDialog(
                    player,
                    SellingItemInfo(insurance, Insurance.PRICE.toLong(), 1),
                    TradeManager.getNewID(),
                    plugin
                )
                    .setOnPayListener { success ->
                        if (success) {
                            val inventory = player.info()!!.inventory.getOrCreate("survivor")
                            if (!inventory.addItem(insurance)) {
                                player.error(getter["trade.error.inventoryFull"])
                                return@setOnPayListener false
                            }
                        } else {
                            player.error(getter["trade.error.poor"])
                        }
                        true
                    }
                    .setOnCancelListener {
                        player.info(getter["trade.cancelled"])
                    }
                    .show()
            }

            modeSwitcher.displayName() -> {
                mode = if (mode == 0.toShort()) 1.toShort() else 0.toShort()
                setMode()
            }

            donate.displayName() -> {
                player.closeInventory()
                Bukkit.getScheduler().runTaskLater(plugin, { _ ->
                    QRUtil.sendToPlayer(CurrencySystem.donation, player)
                }, 20)
            }
        }
    }

    var amount = 1
        private set

    fun subtractOne() {
        inventory.setItem(
            getPositionForLine(4),
            inventory.getItem(getPositionForLine(4))!!
                .also {
                    if (amount > 1) {
                        amount--
                        it.amount = amount
                    } else {
                        amount = 64
                        it.amount = amount
                    }
                }
        )
    }

    fun plusOne() {
        inventory.setItem(
            getPositionForLine(4),
            inventory.getItem(getPositionForLine(4))!!
                .also { stack ->
                    if (amount < 64) {
                        amount++
                        stack.amount = amount
                        val t = stack.itemMeta!!.lore()!!

                        stack.updateItemMeta<ItemMeta> {
                            lore(lore()!!.apply {
                                if (size == 2) {
                                    t.removeAt(1)
                                }
                            })
                        }
                    } else {
                        amount++
                        stack.amount = 1
                        stack.updateItemMeta<ItemMeta> {
                            lore(lore()!!.apply {
                                val text = "兑换${amount}个".toInfoMessage()
                                if (isEmpty())
                                    add(text)
                                else this[0] = text
                            })
                        }
                    }
                }
        )
    }

    fun confirm() {
        PlayerManager.findInfoByPlayer(player)?.let { it.npcTradeCount ++ }

        TradeManager.sell(
            seller = "server",
            what = ItemStack(this.selectedItem!!.type, 1),
            amount = this.amount * if (mode == 0.toShort()) -1 else 1,
            unitPrise = transMap[this.selectedItem!!.type]!!,
            buyer = player
        )

        player.closeInventory()
    }
}