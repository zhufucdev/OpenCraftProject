package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.Prise
import com.zhufu.opencraft.getter
import com.zhufu.opencraft.special_item.dynamic.SpecialItem
import com.zhufu.opencraft.special_item.dynamic.locate.PlayerLocation
import com.zhufu.opencraft.special_item.static.WrappedItem
import com.zhufu.opencraft.toInfoMessage
import com.zhufu.opencraft.updateItemMeta
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta

abstract class CurrencyItem<T : Prise<*>>(material: Material) : WrappedItem(material) {
    abstract val nameCode: String

    override fun onCreate(owner: Player, vararg args: Any) {
        super.onCreate(owner, *args)
        updateItemMeta<ItemMeta> {
            setDisplayName(owner.getter()[nameCode].toInfoMessage())
        }
    }

    abstract val unitValue: T
    val value get() = unitValue * amount
}