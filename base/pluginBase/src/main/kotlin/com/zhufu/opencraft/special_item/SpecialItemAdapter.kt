package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.toComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.scoreboard.Objective
import java.util.UUID

open class SpecialItemAdapter(
    val name: String,
    val langName: String? = null,
    val material: Material,
    val make: ((ItemStack, Language.LangGetter) -> Unit)? = null,
    val deserialize: ((ItemStack, ConfigurationSection, Language.LangGetter) -> Unit)? = null,
    val serialize: ((ItemStack, ConfigurationSection) -> Unit)? = null,
    val tick: ((AdaptedItem, PlayerModifier, YamlConfiguration, Objective, Int) -> Unit)? = null
) {
    class AdaptedItem : StatefulSpecialItem {
        val adapter: SpecialItemAdapter

        constructor(adapter: SpecialItemAdapter, getter: Language.LangGetter)
                : super(adapter.material, getter, UUID.randomUUID()) {
            this.adapter = adapter
            adapter.make?.invoke(this, getter)
            if (adapter.langName != null && (!hasItemMeta() || !itemMeta.hasDisplayName())) {
                updateItemMeta<ItemMeta> {
                    displayName(getter[adapter.langName].toComponent().color(NamedTextColor.AQUA))
                }
            }
        }

        constructor(
            config: ConfigurationSection,
            adapter: SpecialItemAdapter,
            getter: Language.LangGetter,
            id: UUID = UUID.randomUUID()
        ) : super(adapter.material, getter, id) {
            this.adapter = adapter
            adapter.make?.invoke(this, getter)
            if (adapter.langName != null && (!hasItemMeta() || !itemMeta.hasDisplayName())) {
                updateItemMeta<ItemMeta> {
                    displayName(getter[adapter.langName].toComponent().color(NamedTextColor.AQUA))
                }
            }
            adapter.deserialize?.invoke(this, config, getter)
        }

        override fun tick(
            mod: PlayerModifier,
            data: YamlConfiguration,
            score: Objective,
            scoreboardSorter: Int
        ) {
            val old = itemMeta.clone()
            adapter.tick?.invoke(this, mod, data, score, scoreboardSorter) ?: return
            if (itemMeta != old) {
                mod.player.inventory.setItem(inventoryPosition, this)
            }
        }
    }

    fun isThis(itemStack: ItemStack) = StatefulSpecialItem[itemStack].let { it is AdaptedItem && it.adapter.name == this.name }
}