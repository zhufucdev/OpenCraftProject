package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.Language
import com.zhufu.opencraft.getter
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

abstract class SpecialItem(m: Material, val getter: Language.LangGetter) : ItemStack(m) {
    enum class Type {
        FlyingWand, Portal, Coin
    }

    companion object {
        fun isSpecial(config: ConfigurationSection?): Boolean {
            return config?.getBoolean("isSpecialItem", false) ?: false
        }

        fun isSpecial(item: ItemStack) = Portal.isThis(item) || FlyWand.isThis(item) || Coin.isThis(item)

        fun getByConfig(config: ConfigurationSection, getter: Language.LangGetter) = if (isSpecial(config)) {
            when {
                FlyWand.isThis(config) -> FlyWand.deserialize(config, getter)
                Coin.isThis(config) -> Coin.deserialize(config, getter)
                else -> Portal.deserialize(config, getter)
            }
        } else null

        fun getByItem(item: ItemStack, getter: Language.LangGetter) = if (isSpecial(item)) {
            when {
                FlyWand.isThis(item) -> FlyWand(item, getter)
                Coin.isThis(item) -> Coin(item.amount, getter)
                else -> Portal(getter, item)
            }
        } else null

        fun getSerialize(item: ItemStack, getter: Language.LangGetter) =
            getByItem(item, getter)?.getSerialize()?:YamlConfiguration().apply { set("item", item) }

        fun getAll(player: Player): List<SpecialItem> {
            val r = ArrayList<SpecialItem>()
            val getter = player.getter()
            player.inventory.forEach {
                r.add(
                    getByItem(it, getter) ?: return@forEach
                )
            }
            return r
        }
    }

    var inventoryPosition: Int = -1
    abstract val type: Type
    open fun getSerialize(): ConfigurationSection {
        return YamlConfiguration().apply {
            set("isSpecialItem", true)
            set("type", type.name)
        }
    }
}