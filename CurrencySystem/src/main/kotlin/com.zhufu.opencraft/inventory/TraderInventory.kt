package com.zhufu.opencraft.inventory

import com.zhufu.opencraft.*
import com.zhufu.opencraft.CurrencySystem.Companion.transMap
import com.zhufu.opencraft.TextUtil
import com.zhufu.opencraft.special_item.FlyWand
import com.zhufu.opencraft.special_item.Insurance
import com.zhufu.opencraft.special_item.Portal
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class TraderInventory(val player: Player) {
    private val getter = player.getter()
    private val donater: ItemStack = ItemStack(Material.GOLDEN_APPLE).updateItemMeta<ItemMeta> {
        setDisplayName(TextUtil.success("捐赠"))
        lore = listOf("为了表示感谢，我们会给予您一定数量的货币", TextUtil.info("货币数量=在线时长(分钟)*捐赠金额*3"))
    }
    val inventory = Bukkit.createInventory(null, 36, EveryThing.traderInventoryName)
        .apply {
            setItem(size - 9, FlyWand(getter))
            setItem(size - 8, Portal(getter))
            setItem(size - 7, Insurance(getter, player.name))
            setItem(size - 6, donater)
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
            player.sendMessage(arrayOf(TextUtil.error(Language.getDefault("player.error.unknown"))))
            return
        }
        info.inventory.create("survivor").load(false, true)
        player.openInventory(inventory)
    }

    var selectedItem: ItemStack? = null
    private fun setMode() {
        fun getText(num: Int): String = TextUtil.getColoredText(
            "汇率为${if (mode == 0.toShort()) "1:$num" else "$num:1"}",
            TextUtil.TextColor.GOLD,
            false,
            true
        )

        var i = (9 - transMap.size) / 2f.roundToInt()
        transMap.entries.forEach {
            if (it.value != -1L) {
                inventory.setItem(
                    i,
                    ItemStack(it.key, if (it.value <= 64) it.value.toInt() else 1)
                        .also { item ->
                            val meta = item.itemMeta
                            meta!!.lore = listOf(getText(it.value.toInt()))
                            item.itemMeta = meta
                        }
                )
                i++
            }
        }
        inventory.setItem(
            inventory.size - 1,
            if (mode == 0.toShort()) {
                ItemStack(Material.DROPPER).also {
                    it.itemMeta = it.itemMeta!!.also { meta ->
                        meta.setDisplayName(TextUtil.info("兑换模式"))
                        meta.lore = listOf(TextUtil.info("从背包兑换矿石成货币"), TextUtil.tip("点击切换"))
                    }
                }
            } else {
                ItemStack(Material.CHEST).also {
                    it.itemMeta = it.itemMeta!!.also { meta ->
                        meta.setDisplayName(TextUtil.info("购买模式"))
                        meta.lore = listOf(TextUtil.info("用货币购买矿石到背包"), TextUtil.tip("点击切换"))
                    }
                }
            }.also { modeSwitcher = it }
        )
    }

    fun select(current: ItemStack) {
        amount = 1
        selectedItem = current.clone()
        inventory.setItem(getPositionForLine(2),
            ItemStack(Material.NETHER_STAR)
                .also { itemStack ->
                    itemStack.itemMeta =
                        itemStack.itemMeta!!
                            .also { it.setDisplayName("少${if (mode == 0.toShort()) "兑换" else "购买"}一个") }
                }
        )
        inventory.setItem(getPositionForLine(6),
            ItemStack(Material.NETHER_STAR)
                .also { itemStack ->
                    itemStack.itemMeta =
                        itemStack.itemMeta!!
                            .also { it.setDisplayName("多${if (mode == 0.toShort()) "兑换" else "购买"}一个") }
                }
        )
        inventory.setItem(
            getPositionForLine(4),
            selectedItem!!
                .also {
                    it.amount = 1
                    val meta = it.itemMeta
                    meta!!.setDisplayName(TextUtil.getColoredText("点击确认", TextUtil.TextColor.GOLD, true, false))
                    it.itemMeta = meta
                }
        )
    }

    fun selectSpecialItem(current: ItemStack) {
        when {
            FlyWand.isThis(current) -> {
                val price = FlyWand.MAX_TIME_REMAINING * FlyWand.PRICE_PER_MIN / 60
                PaymentDialog(
                    player,
                    SellingItemInfo(FlyWand(getter), price.roundToLong(), 1),
                    TradeManager.getNewID(),
                    CurrencySystem.instance
                ).setOnPayListener { success ->
                    if (success) {
                        val info = player.info()!!

                        val survivor = info.inventory.create("survivor")
                        if (survivor.any { item -> FlyWand.isThis(item) }) {
                            player.sendMessage(
                                arrayOf(
                                    TextUtil.error("抱歉，但你不能同时拥有两支权杖"),
                                    TextUtil.tip("为了同时拥有两支权杖，您可以尝试使用箱子等容器，但这并不会带来好的游戏体验")
                                )
                            )
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

            Portal.isThis(current) -> {
                PaymentDialog(
                    player,
                    SellingItemInfo(Portal(getter), Portal.PRICE.toLong(), 1),
                    TradeManager.getNewID(),
                    CurrencySystem.instance
                ).setOnPayListener { success ->
                    val info = player.info()!!
                    if (success) {
                        val survivor = info.inventory.create("survivor")
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

            Insurance.isThis(current) -> {
                val insurance = Insurance(getter, player.name)
                PaymentDialog(
                    player,
                    SellingItemInfo(insurance, Insurance.PRICE.toLong(), 1),
                    TradeManager.getNewID(),
                    CurrencySystem.instance
                )
                    .setOnPayListener { success ->
                        if (success) {
                            val inventory = player.info()!!.inventory.create("survivor")
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

            current == modeSwitcher -> {
                mode = if (mode == 0.toShort()) 1.toShort() else 0.toShort()
                setMode()
            }
            current == donater -> {
                player.closeInventory()
                Bukkit.getScheduler().runTaskLater(CurrencySystem.instance, { _ ->
                    QRUtil.sendToPlayer(CurrencySystem.donation, player)
                }, 20)
            }
        }
    }

    var amount = 1
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
                        val t = stack.itemMeta!!.lore!!
                        if (t.size == 2) {
                            t.removeAt(1)
                        }
                        stack.itemMeta = stack.itemMeta!!.also { it.lore = t }
                    } else {
                        amount++
                        stack.amount = 1
                        val t = stack.itemMeta!!.lore!!
                        val text = TextUtil.info("兑换${amount}个")
                        if (t.isEmpty())
                            t.add(text)
                        else t[0] = text
                        stack.itemMeta = stack.itemMeta!!.also { it.lore = t }
                    }
                }
        )
    }

    fun confirm() {
        val tag = PlayerManager.findInfoByPlayer(player)
            ?.tag ?: return
        tag.set("npcTrade", tag.getInt("npcTrade") + 1)

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