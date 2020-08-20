package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.GeneralPrise
import org.bukkit.Material

class Coin : GeneralCurrency(Material.GOLD_NUGGET) {

    override val unitValue
        get() = GeneralPrise(10)

    override val nameCode: String
        get() = "trade.coin.name"
}