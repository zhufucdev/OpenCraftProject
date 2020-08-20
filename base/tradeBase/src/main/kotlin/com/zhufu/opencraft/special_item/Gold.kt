package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.GeneralPrise
import org.bukkit.Material

class Gold : GeneralCurrency(Material.GOLD_INGOT) {
    override val unitValue
        get() = GeneralPrise(400)

    override val nameCode: String
        get() = "trade.gold.name"
}