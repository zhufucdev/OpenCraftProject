package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.GeneralPrise
import org.bukkit.Material

class CopperCoin : GeneralCurrency(Material.IRON_NUGGET) {
    override val unitValue
        get() = GeneralPrise(1)

    override val nameCode: String
        get() = "trade.cc.name"
}