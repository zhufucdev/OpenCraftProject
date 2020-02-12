package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class WorldUI(plugin: Plugin, info: Info, override val parentInventory: ClickableInventory) : PageInventory<WorldUI.Adapter>(
    title = getLang(info, "ui.world.title").toInfoMessage(),
    adapter = Adapter(WorldManager.getAvailableWorlds().filter { it.permission.canUse(info.player) }, info.getter()),
    itemsOnePage = 36,
    plugin = plugin
), Backable {
    class Adapter(val worlds: List<WorldManager.mWorld>, private val getter: Language.LangGetter) :
        PageInventory.Adapter() {
        override val size: Int
            get() = worlds.size
        override val hasToolbar: Boolean
            get() = true

        override fun getItem(index: Int, currentPage: Int): ItemStack =
            ItemStack(Material.GRASS_BLOCK).updateItemMeta<ItemMeta> {
                val world = worlds[index]
                setDisplayName(world.world.name.toSuccessMessage())
                val l = arrayListOf<String>()
                if (world.description.isNotEmpty())
                    TextUtil.formatLore(world.description).forEach {
                        l.add(it.toInfoMessage())
                    }
                l.add(getter["ui.world.tip"].toTipMessage())
                lore = l
            }

        override fun getToolbarItem(index: Int): ItemStack =
            if (index == 6) {
                Widgets.back.updateItemMeta<ItemMeta> {
                    setDisplayName(getter["ui.back"].toInfoMessage())
                }
            } else {
                super.getToolbarItem(index)
            }
    }

    init {
        setOnItemClickListener { index, _ ->
            val to = adapter.worlds[index].world.spawnLocation
            val event = PlayerTeleportedEvent(
                info.player,
                info.player.location,
                to
            )
            Bukkit.getPluginManager().callEvent(event)
            if (!event.isCancelled) {
                info.player.teleport(to)
            }
        }
        setOnToolbarItemClickListener { index, _ ->
            if (index == 6) {
                back(info.player)
            }
        }
    }
}