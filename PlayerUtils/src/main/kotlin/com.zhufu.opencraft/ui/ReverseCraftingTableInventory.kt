package com.zhufu.opencraft.ui

import com.zhufu.opencraft.IntractableInventory
import com.zhufu.opencraft.Widgets
import com.zhufu.opencraft.special_item.SpecialItem
import com.zhufu.opencraft.updateItemMeta
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.toErrorMessage
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toTipMessage
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class ReverseCraftingTableInventory(val getter: Language.LangGetter, plugin: Plugin) : IntractableInventory(plugin) {
    private var recipeIndex = 0
    private var recipesCount = 0
    private var reverseCost = 0
    private var validate = false

    override val inventory = Bukkit.createInventory(null, 27, Component.text(getter["rct.name"]))

    init {
        builder {
            val panel = ItemStack(Material.GRAY_STAINED_GLASS_PANE).updateItemMeta<ItemMeta> {
                displayName(getter["rct.panel.title"].toInfoMessage())
                lore(listOf(getter["rct.panel.subtitle"].toTipMessage()))
            }
            fill(0..2, 0..2, panel)
            set(1, 1, null)
        }
        updateTipItem()
    }

    override fun onClick(event: InventoryClickEvent) {
        event.isCancelled = false
        if (event.clickedInventory != inventory) {
            // shift click -> move to center slot
            if (event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    val material = materialItem
                    if (material == null) {
                        val original = inventory.getItem(3)
                        inventory.setItem(3, null)
                        setMaterialItem(original)
                        recipeIndex = 0
                        updateRecipe(original)
                        updateTipItem()
                    } else {
                        returnItems(event.whoClicked, arrayOf(material))
                    }
                })
            }
        } else if (event.rawSlot == 10) {
            // drag-like
            recipeIndex = 0
            if (event.action.name.startsWith("PICK")
                || event.action == InventoryAction.PLACE_ONE
                || event.action == InventoryAction.PLACE_SOME
            ) {
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    updateRecipe(inventory.getItem(10))
                }, 3)
                updateTipItem()
            } else {
                updateRecipe(event.cursor)
                updateTipItem()
            }
        } else if (event.rawSlot == 13) {
            // recipe iterator
            event.isCancelled = true
            if (recipesCount < 2) {
                return
            }
            recipeIndex++
            if (recipeIndex > recipesCount - 1) {
                recipeIndex = 0
            }

            updateRecipe(inventory.getItem(10))
            updateTipItem()
        } else if (validate && event.rawSlot % 9 in 6..8) {
            // pick indigent
            if (recipesCount > 0) {
                setMaterialItem(materialItem?.apply { amount -= reverseCost })
            }
            recipesCount = 0
            updateTipItem()
        } else {
            event.isCancelled = true
        }
    }

    private fun returnItems(player: HumanEntity, list: Array<ItemStack>) {
        val notFit = player.inventory.addItem(*list)
        notFit.values.forEach {
            player.world.dropItemNaturally(player.eyeLocation, it)
        }
    }

    override fun onClose(player: HumanEntity) {
        if (recipesCount == 0) {
            // can't craft or already crafted
            returnItems(
                player,
                buildList {
                    materialItem?.let { add(it) }
                    for (x in 6..8) {
                        for (y in 0..2) {
                            inventory.getItem(x + y * 9)?.let { add(it) }
                        }
                    }
                }.toTypedArray()
            )
        } else {
            // hasn't crafted, just put material in place
            returnItems(player, arrayOf(materialItem ?: return))
        }
    }

    private fun setMaterialItem(itemStack: ItemStack?) = inventory.setItem(10, itemStack)
    private val materialItem get() = inventory.getItem(10)
    private fun updateTipItem() {
        builder {
            set(4, 1,
                ItemStack(Material.CRAFTING_TABLE)
                    .asQuantity(recipeIndex + 1)
                    .updateItemMeta<ItemMeta> {
                        displayName(getter["rct.reverse.title"].toInfoMessage())
                        if (recipesCount > 1) {
                            lore(listOf(getter["rct.reverse.tip"].toTipMessage()))
                        }
                    }
            )
        }
    }

    private fun updateRecipe(material: ItemStack?) {
        if (material == null || material.type == Material.AIR) {
            recipesCount = 0
            drawResults(null, null)
        } else {
            val recipes = Bukkit.getRecipesFor(material).filter { it is ShapedRecipe || it is ShapelessRecipe }
            recipesCount = recipes.size
            drawResults(
                if (recipeIndex < recipes.size)
                    recipes[recipeIndex]
                else
                    null,
                material
            )
        }
    }

    private fun drawResults(recipe: Recipe?, material: ItemStack?) {
        builder {
            fill(6..8, 0..2, null)
        }
        if (recipe == null || material == null) {
            return
        }

        validate = true
        fun reportError(baseItem: ItemStack, msgCode: String) {
            builder {
                set(7, 1, baseItem.updateItemMeta<ItemMeta> {
                    displayName(getter["rct.error.$msgCode"].toErrorMessage())
                })
            }
            validate = false
        }

        if (
            (material.itemMeta is Damageable && (material.itemMeta as Damageable).hasDamage())
            || SpecialItem.isSpecial(material)
        ) {
            reportError(Widgets.close, "unhandled")
            return
        }

        val baseCost = recipe.result.amount
        if (material.amount < baseCost) {
            reportError(ItemStack(Material.DEAD_BUSH), "poor")
            return
        }
        val output = material.amount / baseCost
        val totalCost = output * baseCost
        reverseCost = totalCost

        when (recipe) {
            is ShapedRecipe -> {
                builder {
                    recipe.shape.forEachIndexed { y, row ->
                        row.forEachIndexed { x, s ->
                            val indignant = recipe.ingredientMap[s]
                            indignant?.amount = output
                            set(x + 6, y, indignant)
                        }
                    }
                }
            }

            is ShapelessRecipe -> {
                recipe.ingredientList.forEachIndexed { i, item ->
                    val index = if (i < 3) i + 6 else if (i < 6) i + 12 else i + 18
                    item.amount = output
                    inventory.setItem(index, item)
                }
            }

            else -> {
                reportError(Widgets.close, "unhandled")
            }
        }
    }
}