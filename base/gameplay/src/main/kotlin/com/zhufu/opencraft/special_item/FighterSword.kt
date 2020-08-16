package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import org.bukkit.inventory.meta.ItemMeta

abstract class FighterSword: SingleDirectionEnchantEquipment() {
    override val maxLevel: Int
        get() = 4

    override fun updateDisplay() {
        val getter = Language.LangGetter(owner)
        itemStack.updateItemMeta<ItemMeta> {
            setDisplayName(getter["rpg.sword.name"].toInfoMessage())
            lore = TextUtil.formatLore(getter["rpg.sword.subtitle"])
                .map { it.toTipMessage() }
                .plus(getter["rpg.level", level])
        }
        itemLocation.push()
    }
}