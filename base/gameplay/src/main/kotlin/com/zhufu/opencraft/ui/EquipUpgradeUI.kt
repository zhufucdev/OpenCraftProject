package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.special_item.Upgradable
import com.zhufu.opencraft.special_item.dynamic.SpecialItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class EquipUpgradeUI(plugin: Plugin, owner: Info) :
    DraggableInventory(plugin, 3, owner.getter()["rpg.ui.upgrade.title"].toInfoMessage()) {

    init {
        val getter = owner.getter()
        for (y in 0 .. 2) {
            (0 .. 2).forEach { x ->
                val item = ItemStack(Material.PURPLE_STAINED_GLASS_PANE).updateItemMeta<ItemMeta> {
                    // Visual
                    val header = "rpg.ui.upgrade.place"
                    val base = getter["$header.content"]
                    with(Language) {
                        setDisplayName(
                            when (x) {
                                0 -> when (y) {
                                    0 -> base + getDefault("$header.right_down")
                                    1 -> base + getDefault("$header.right")
                                    else -> base + getDefault("$header.right_up")
                                }
                                1 -> when (y) {
                                    0 -> getDefault("$header.down").let { it + base + it }
                                    1 -> return@forEach
                                    else -> getDefault("$header.up").let { it + base + it }
                                }
                                else -> when (y) {
                                    0 -> getDefault("$header.left_down") + base
                                    1 -> getDefault("$header.left") + base
                                    else -> getDefault("$header.left_up") + base
                                }
                            }.toInfoMessage()
                        )
                    }
                    addEnchant(Enchantment.ARROW_INFINITE, 1, true)
                    addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
                }
                setItem(x, y, item)
            }
        }
    }

    private var cost = 0
    override fun onPlace(event: InventoryClickEvent) {
        if (event.rawSlot != 10) return
        val si = SpecialItem[event.cursor ?: return]
        if (si is Upgradable) {
            if (si.level >= si.maxLevel) {
                setItem(7, 1,
                    ItemStack(Material.BARRIER).updateItemMeta<ItemMeta> {
                        val getter = event.whoClicked.getter()
                        setDisplayName(getter["rpg.error.maxLevel.title", event.cursor!!.itemMeta.displayName].toErrorMessage())
                        lore = TextUtil.formatLore(getter["rpg.error.maxLevel.subtitle"]).map { it.toInfoMessage() }
                    })
            } else {
                setItem(5, 1, ItemStack(Material.EXPERIENCE_BOTTLE).updateItemMeta<ItemMeta> {
                    val getter = event.whoClicked.getter()
                    cost = si.exp(si.level + 1)
                    setDisplayName(getter["rpg.ui.upgrade.tip.cost", cost])
                    lore = TextUtil.formatLore(getter["rpg.ui.upgrade.tip.pick"]).map { it.toTipMessage() }
                })
                setItem(
                    7, 1,
                    SpecialItem.make(
                        name = si::class.simpleName!!,
                        owner = Bukkit.getPlayer(event.whoClicked.uniqueId)!!
                    )!!.apply { (this as Upgradable).level = si.level + 1 }.itemLocation.itemStack,
                    false
                )
            }
        }
    }

    override fun onTake(event: InventoryClickEvent) {
        if (event.rawSlot == 10) {
            clear(7, 1)
            clear(5, 1)
        } else if (event.rawSlot == 16) {
            val info = event.whoClicked.info()
            if (info == null) {
                event.isCancelled = true
                event.whoClicked.apply {
                    closeInventory()
                    error(Language.getDefault("player.error.unknown"))
                }
                return
            }
            if (info.exp >= cost) {
                info.exp -= cost

                clear(1, 1)
                clear(5, 1)
            } else {
                event.isCancelled = true
                event.whoClicked.error(info.getter()["rpg.error.lackOfExp", cost, info.exp])
                event.whoClicked.closeInventory()
            }
        }
    }

    override fun onClose() {
        val remainingItems = arrayListOf<ItemStack>()
        fun detect(x: Int, y: Int) {
            val item = inventory.getItem(x + y * 9)
            if (item != null && item.type != Material.AIR) {
                remainingItems.add(item)
            }
        }
        detect(1, 1)
        for (x in 3 until 9) {
            for (y in 0 until 3) {
                if (y == 1 && x == 5 || x == 7)
                    continue
                detect(x, y)
            }
        }
        showing!!.inventory.addItem(*(remainingItems.toTypedArray()))
        inventory.clear()
    }
}