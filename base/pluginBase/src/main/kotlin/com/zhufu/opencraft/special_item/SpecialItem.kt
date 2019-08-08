package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.Info
import com.zhufu.opencraft.Language
import com.zhufu.opencraft.PlayerModifier
import com.zhufu.opencraft.getter
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Objective
import org.reflections.Reflections
import kotlin.jvm.internal.Reflection
import kotlin.reflect.full.companionObjectInstance

abstract class SpecialItem(m: Material, val getter: Language.LangGetter) : ItemStack(m) {
    enum class Type {
        FlyingWand, Portal, Coin, Insurance
    }

    companion object {
        private val reflection by lazy { Reflections("com.zhufu.opencraft.special_item") }

        fun isSpecial(config: ConfigurationSection?): Boolean {
            return config?.getBoolean("isSpecialItem", false) ?: false
        }

        fun isSpecial(item: ItemStack): Boolean {
            reflection.getSubTypesOf(SpecialItem::class.java).forEach {
                if ((Reflection.createKotlinClass(it).companionObjectInstance as SISerializable).isThis(item)) return true
            }
            return false
        }

        fun getByConfig(config: ConfigurationSection, getter: Language.LangGetter): SpecialItem? {
            reflection.getSubTypesOf(SpecialItem::class.java).forEach {
                val instance = Reflection.createKotlinClass(it).companionObjectInstance as SISerializable
                if (instance.isThis(config))
                    return instance.deserialize(config, getter)
            }
            return null
        }

        fun getByItem(item: ItemStack, getter: Language.LangGetter): SpecialItem? {
            reflection.getSubTypesOf(SpecialItem::class.java).forEach {
                val instance = Reflection.createKotlinClass(it).companionObjectInstance as SISerializable
                if (instance.isThis(item))
                    return instance.deserialize(item, getter)
            }
            return null
        }

        fun getSerialize(item: ItemStack, getter: Language.LangGetter) =
            getByItem(item, getter)?.getSerialize() ?: YamlConfiguration().apply { set("item", item) }

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

    open fun doPerTwoSeconds(mod: PlayerModifier, data: YamlConfiguration, score: Objective, scoreboardSorter: Int) {}
}