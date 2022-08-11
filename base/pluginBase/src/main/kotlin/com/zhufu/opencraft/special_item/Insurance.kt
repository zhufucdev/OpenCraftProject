package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toComponent
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

class Insurance(getter: Language.LangGetter, val player: String, val number: Long = System.currentTimeMillis()) :
    SpecialItem(Material.WRITTEN_BOOK, getter) {

    init {
        updateItemMeta<BookMeta> {
            title = TextUtil.info(getter["insurance.name"])
            author = getter["insurance.content.2"]
            addPages(buildString {
                appendLine(getter["insurance.content.1", player])
                for (i in 2..3)
                    appendLine(getter["insurance.content.$i"])
            }.toComponent())

            lore(listOf(player.toComponent(), number.toString().toComponent()))
            addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS)
            isUnbreakable = true
            addUnsafeEnchantment(Enchantment.KNOCKBACK, 1)
        }
    }

    companion object : SISerializable {
        override fun deserialize(itemStack: ItemStack, getter: Language.LangGetter): SpecialItem =
            Insurance(
                getter,
                itemStack.itemMeta?.lore?.firstOrNull() ?: "unknown",
                itemStack.itemMeta?.lore?.get(1)?.toLongOrNull() ?: System.currentTimeMillis()
            )

        override fun deserialize(config: ConfigurationSection, getter: Language.LangGetter): SpecialItem =
            Insurance(
                getter,
                config.getString("player") ?: "unknown",
                config.getLong("number", System.currentTimeMillis())
            )

        override fun isThis(itemStack: ItemStack?): Boolean =
            itemStack != null
                    && itemStack.type == Material.WRITTEN_BOOK
                    && itemStack.hasItemMeta()
                    &&
                    with(itemStack.itemMeta!!) {
                        hasItemFlag(ItemFlag.HIDE_ENCHANTS)
                                && hasItemFlag(ItemFlag.HIDE_UNBREAKABLE)
                                && hasLore()
                    }

        override fun isThis(config: ConfigurationSection): Boolean =
            config.getString("type") == Insurance::class.simpleName

        const val PRICE = 100
    }

    override fun getSerialized(): ConfigurationSection {
        val r = super.getSerialized()
        r["player"] = player
        r["number"] = number
        return r
    }
}