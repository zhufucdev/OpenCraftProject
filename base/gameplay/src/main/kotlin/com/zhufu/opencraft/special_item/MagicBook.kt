package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import com.zhufu.opencraft.special_item.base.BindItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType

class MagicBook : BindItem() {
    override val material: Material
        get() = Material.ENCHANTED_BOOK

    override fun onCreate(owner: Player, vararg args: Any) {
        super.onCreate(owner, *args)
        val getter = owner.getter()
        itemLocation.itemStack.updateItemMeta<ItemMeta> {
            setDisplayName(getter["rpg.magic_book.name"].toInfoMessage())
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        val getter = Language.LangGetter(owner)
        itemStack.updateItemMeta<ItemMeta> {
            lore = TextUtil.formatLore(getter["rpg.magic_book.subtitle"]).map { it.toTipMessage() }
                .plus(getter["rpg.magic_book.available", owner.magic.map { getter[it.nameCode] }.toString(", ")])
        }
        itemLocation.push()
    }

    enum class Magic(private val indicate: Material, val potionType: PotionEffectType, val isHealing: Boolean = false) {
        // Damage
        POISON(Material.PURPLE_DYE, PotionEffectType.POISON), FIRE(Material.RED_DYE, PotionEffectType.LUCK),
        SLOWNESS(Material.LIGHT_GRAY_DYE, PotionEffectType.SLOW), WEAKNESS(Material.GREEN_DYE, PotionEffectType.WEAKNESS),
        INSTANT_DAMAGE(Material.BLUE_DYE, PotionEffectType.HARM),
        // Healing
        HEALING(Material.ORANGE_DYE, PotionEffectType.HEAL, true),
        HEALING_POOL(Material.YELLOW_DYE, PotionEffectType.REGENERATION, true);

        fun indicator(getter: Language.LangGetter): ItemStack =
            ItemStack(indicate).updateItemMeta<ItemMeta> {
                setDisplayName(getter[nameCode].toInfoMessage())
                lore = TextUtil.formatLore(getter[description]).map { it.toTipMessage() }
            }

        val nameCode: String
            get() = "rpg.magic_book.magic.${name.toLowerCase()}.name"
        val description: String
            get() = "rpg.magic_book.magic.${name.toLowerCase()}.subtitle"

    }
}