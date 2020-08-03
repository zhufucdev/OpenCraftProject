package com.zhufu.opencraft

import com.zhufu.opencraft.rpg.Role
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class RoleSelectUI(plugin: Plugin, val getter: Language.LangGetter, val onClick: (Role) -> Unit) :
    ClickableInventory(plugin) {
    override val inventory: Inventory = Bukkit.createInventory(
        null,
        (Math.floorDiv(Role.values().size * 3, 9) + if (Role.values().size % 3 == 0) 0 else 1) * 9,
        getter["rpg.ui.select.title"].toInfoMessage()
    )

    init {
        Role.values().forEachIndexed { index, role ->
            if (role == Role.NULL) return@forEachIndexed
            val item = representation[role]?.invoke() ?: ItemStack(Material.BARRIER)
            item.updateItemMeta<ItemMeta> {
                setDisplayName(getter[role.nameCode].toInfoMessage())
                lore = TextUtil.formatLore(getter[role.description])
            }
            inventory.setItem(index * 3, item)
        }
    }

    private var selected = false
    override fun onClick(event: InventoryClickEvent) {
        if (event.slot % 3 == 0) {
            selected = true
            onClick(Role.values()[event.slot / 3])
            close()
        }
    }

    override fun onClose(player: HumanEntity) {
        if (!selected) {
            Bukkit.getScheduler().runTask(plugin) { _ ->
                RoleSelectUI(plugin, getter, onClick).show(player)
            }
        }
    }

    companion object {
        private val representation = mapOf(
            Role.MAGICIAN to { ItemStack(Material.ENCHANTED_BOOK) },
            Role.RANGER to { ItemStack(Material.BOW) },
            Role.PRIEST to {
                ItemStack(Material.POTION).updateItemMeta<PotionMeta> {
                    addCustomEffect(PotionEffect(PotionEffectType.HEAL, Int.MAX_VALUE, 1), false)
                    addItemFlags(ItemFlag.HIDE_POTION_EFFECTS)
                }
            },
            Role.SUMMONER to { ItemStack(Material.ENDERMAN_SPAWN_EGG) }
        )
    }
}