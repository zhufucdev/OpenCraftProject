package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import com.zhufu.opencraft.special_item.base.BindItem
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.ItemMeta

abstract class FighterSword: BindItem(), Upgradable {
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

    override fun onCreate(owner: Player, vararg args: Any) {
        super.onCreate(owner, *args)
        val lvlArg = args.firstOrNull()
        if (lvlArg is Int) {
            level = lvlArg
        } else {
            updateEnchantment()
        }
        itemLocation.itemStack.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
    }

    private var mLevel = 1
    override var level: Int
        get() = mLevel
        set(value) {
            mLevel = value
            updateEnchantment()
            updateDisplay()
        }

    abstract fun updateEnchantment()

    protected fun ItemMeta.setEnchants(vararg enchants: Pair<Enchantment, Int>) {
        enchants.forEach { (e, _) ->
            removeEnchant(e)
        }
        enchants.forEach { (e, l) ->
            addEnchant(e, l, true)
        }
    }
}