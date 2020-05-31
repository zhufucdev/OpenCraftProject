package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.scoreboard.Objective
import kotlin.concurrent.thread

open class SpecialItemAdapter(
    val name: String,
    val langName: String? = null,
    val material: Material,
    val make: ((ItemStack, Language.LangGetter) -> Unit)? = null,
    val deserialize: ((ItemStack, ConfigurationSection, Language.LangGetter) -> Unit)? = null,
    val serialize: ((ItemStack, ConfigurationSection) -> Unit)? = null,
    val judgeFromItem: ((ItemStack) -> Boolean)? = null,
    val judgeFromConfig: ((ConfigurationSection) -> Boolean)? = null,
    val tick: ((AdapterItem, PlayerModifier, YamlConfiguration, Objective, Int) -> Unit)? = null
) {
    class AdapterItem : SpecialItem {
        val adapter: SpecialItemAdapter

        constructor(adapter: SpecialItemAdapter, getter: Language.LangGetter) : super(adapter.material, getter) {
            this.adapter = adapter
            adapter.make?.invoke(this, getter)
            if (adapter.langName != null && (!hasItemMeta() || !itemMeta.hasDisplayName())) {
                updateItemMeta<ItemMeta> {
                    setDisplayName(TextUtil.getColoredText(getter[adapter.langName], TextUtil.TextColor.AQUA))
                }
            }
        }

        constructor(adapter: SpecialItemAdapter, item: ItemStack, getter: Language.LangGetter)
                : super(adapter.material, getter) {
            this.adapter = adapter
            itemMeta = item.itemMeta
        }

        override fun doPerTick(
            mod: PlayerModifier,
            data: YamlConfiguration,
            score: Objective,
            scoreboardSorter: Int
        ) {
            val old = itemMeta.clone()
            adapter.tick?.invoke(this, mod, data, score, scoreboardSorter)
            if (itemMeta != old) {
                mod.player.inventory.setItem(inventoryPosition, this)
            }
        }

        override fun getSerialized(): ConfigurationSection = YamlConfiguration().apply {
            set("isSpecialItem", true)
            set("type", adapter.name)
            adapter.serialize?.invoke(this@AdapterItem, this)
        }
    }

    fun isThis(item: ItemStack): Boolean = judgeFromItem?.invoke(item)
        ?: (item.type == material && langName != null
                && item.hasItemMeta() && item.itemMeta.hasDisplayName()
                && Language.languages.any {
            TextUtil.getColoredText(
                it.getString(langName)!!,
                TextUtil.TextColor.AQUA
            ) == item.itemMeta.displayName
        })

    fun isThis(config: ConfigurationSection) = judgeFromConfig?.invoke(config)
            ?: config.getString("type") == name

    fun getAdaptItem(from: ItemStack, holder: Player) = AdapterItem(this, from, holder.getter())
}