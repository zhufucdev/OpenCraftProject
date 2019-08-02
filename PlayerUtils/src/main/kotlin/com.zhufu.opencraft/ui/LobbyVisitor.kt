package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.lobby.PlayerLobby
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.Plugin

class LobbyVisitor(plugin: Plugin, private val info: Info) : PageInventory<LobbyVisitor.Adapter>(
    title = info.getter()["ui.visitor.title"],
    plugin = plugin,
    adapter = Adapter(info),
    itemsOnePage = 36
) {
    private val list get() = adapter.list

    class Adapter(private val info: Info) : PageInventory.Adapter() {
        val list = PlayerLobbyManager.list().filter { it.owner != info }
        val getter: Language.LangGetter = info.getter()
        override val size: Int
            get() = list.size + if (!PlayerLobbyManager.isInOwnLobby(info)) 1 else 0

        override fun getItem(index: Int, currentPage: Int): ItemStack =
            if (index < list.size)
                list[index].let {
                    ItemStack(Material.PLAYER_HEAD).updateItemMeta<SkullMeta> {
                        owningPlayer = it.owner.offlinePlayer
                        setDisplayName(it.owner.name)
                        lore = listOf(getter["ui.visitor.located", "(${it.x}, ${it.z})"].toInfoMessage())
                    }
                }
            else
                Widgets.back.updateItemMeta<ItemMeta> {
                    setDisplayName(getter["ui.visitor.back"].toInfoMessage())
                }
    }

    init {
        setOnItemClickListener { index, _ ->
            val item: PlayerLobby
             = if (index < list.size) {
                list[index]
            } else {
                PlayerLobbyManager[info]
            }
            item.tpThere(info.player)
        }
    }
}