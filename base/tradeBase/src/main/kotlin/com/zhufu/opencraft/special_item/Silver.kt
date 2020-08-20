package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.GeneralPrise
import org.bukkit.Material

class Silver : GeneralCurrency(Material.IRON_INGOT) {
    override val unitValue
        get() = GeneralPrise(100)

    override val nameCode: String
        get() = "trade.silver.name"
}