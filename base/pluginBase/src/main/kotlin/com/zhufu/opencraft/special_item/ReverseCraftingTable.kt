package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.Base
import com.zhufu.opencraft.special_block.ReverseCraftingTableBlock
import com.zhufu.opencraft.updateItemMeta
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.toComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.meta.ItemMeta
import java.util.UUID
import kotlin.reflect.KClass

class ReverseCraftingTable : StatelessSpecialItem, Placeable {
    constructor(getter: Language.LangGetter) : super(Material.SMITHING_TABLE, SIID) {
        updateMeta(getter)
    }

    constructor(item: ItemStack): super(Material.SMITHING_TABLE, SIID) {
        this.amount = item.amount
        this.itemMeta = item.itemMeta
    }

    override val block: KClass<*>
        get() = ReverseCraftingTableBlock::class

    override fun updateMeta(getter: Language.LangGetter) {
        updateItemMeta<ItemMeta> {
            displayName(getter["rct.name"].toComponent().color(NamedTextColor.LIGHT_PURPLE))
        }
    }

    companion object : StatelessSICompanion {
        override val SIID: UUID get() = UUID.fromString("B9673B91-6BDA-491E-9D97-86C81A1E8CE4")

        override fun newInstance(getter: Language.LangGetter, madeFor: Player) =
            ReverseCraftingTable(getter)

        override fun fromItemStack(item: ItemStack) = ReverseCraftingTable(item)

        override val recipe: Recipe
            get() = ShapedRecipe(Base.namespacedKey, ReverseCraftingTable(Language.LangGetter.default))
                .shape(
                    "xx",
                    "oo",
                    "oo"
                )
                .setIngredient('x', Material.NETHERITE_INGOT)
                .setIngredient('o', RecipeChoice.MaterialChoice(Tag.PLANKS))
    }
}