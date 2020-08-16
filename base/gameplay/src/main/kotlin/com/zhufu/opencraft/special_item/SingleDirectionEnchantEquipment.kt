package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.special_item.base.BindItem
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta

abstract class SingleDirectionEnchantEquipment: BindItem(), Upgradable {
    protected fun ItemMeta.setEnchants(vararg enchants: Pair<Enchantment, Int>) {
        enchants.forEach { (e, _) ->
            removeEnchant(e)
        }
        enchants.forEach { (e, l) ->
            addEnchant(e, l, true)
        }
    }

    override fun onCreate(owner: Player, vararg args: Any) {
        super.onCreate(owner, *args)
        val lvlArg = args.firstOrNull()
        if (lvlArg is Int) {
            level = lvlArg
        } else {
            updateEnchantment()
        }
        updateDisplay()
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
}