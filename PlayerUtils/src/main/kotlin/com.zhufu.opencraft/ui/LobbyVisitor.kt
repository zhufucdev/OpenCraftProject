package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.lobby.PlayerLobby
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.Plugin
import kotlin.math.absoluteValue

class LobbyVisitor(plugin: Plugin, private val info: Info) : PageInventory<LobbyVisitor.Adapter>(
    title = info.getter()["ui.visitor.title"],
    plugin = plugin,
    adapter = Adapter(info),
    itemsOnePage = 36
) {
    private val list get() = adapter.list
    private val review get() = adapter.review
    private val currentLobby get() = PlayerLobbyManager.targetOf(info.player)!!

    class Adapter(private val info: Info) : PageInventory.Adapter() {
        val list = PlayerLobbyManager.list().filter { it.owner != info }.sortedByDescending { it.likesInAll }
        val getter: Language.LangGetter = info.getter()
        override val size: Int
            get() = list.size + if (!PlayerLobbyManager.isInOwnLobby(info)) 1 else 0
        override val hasToolbar: Boolean
            get() = true
        val review get() = PlayerLobbyManager.targetOf(info.player)!!.reviewedBy(info)

        private fun getReviewMsg(forWhich: PlayerLobby) =
            getter["lobby.get.${if (forWhich.likesInAll >= 0) "like" else "dislike"}", forWhich.likesInAll.absoluteValue]
                .toSuccessMessage()

        override fun getItem(index: Int, currentPage: Int): ItemStack =
            if (index < list.size)
                list[index].let {
                    ItemStack(Material.PLAYER_HEAD).updateItemMeta<SkullMeta> {
                        owningPlayer = it.owner.offlinePlayer
                        setDisplayName(it.owner.name ?: getter["player.unknownName"])
                        lore = listOf(
                            getter["ui.visitor.located", "(${it.x}, ${it.z})"].toInfoMessage(),
                            getReviewMsg(it)
                        )
                    }
                }
            else
                Widgets.back.updateItemMeta<ItemMeta> {
                    setDisplayName(getter["ui.visitor.back"].toInfoMessage())
                }

        override fun getToolbarItem(index: Int): ItemStack {
            return if (!PlayerLobbyManager.isInOwnLobby(info)) {
                when (index) {
                    6 -> ItemStack(Material.APPLE).updateItemMeta<ItemMeta> {
                        setDisplayName(getter["lobby.like"].toInfoMessage())
                        if (review == true) {
                            lore = listOf(getter["lobby.cancel"].toTipMessage())
                            addEnchant(Enchantment.ARROW_INFINITE, 1, true)
                            addItemFlags(ItemFlag.HIDE_ENCHANTS)
                        }
                    }
                    5 -> ItemStack(Material.POISONOUS_POTATO).updateItemMeta<ItemMeta> {
                        setDisplayName(getter["lobby.dislike"].toErrorMessage())
                        if (review == false) {
                            lore = listOf(getter["lobby.cancel"].toTipMessage())
                            addEnchant(Enchantment.ARROW_INFINITE, 1, true)
                            addItemFlags(ItemFlag.HIDE_ENCHANTS)
                        }
                    }
                    else -> super.getToolbarItem(index)
                }
            } else if (index == 6) {
                info.skullItem.updateItemMeta<ItemMeta> {
                    setDisplayName(getter["lobby.yours"].toInfoMessage())
                    lore = listOf(getReviewMsg(PlayerLobbyManager[info]))
                }
            } else {
                super.getToolbarItem(index)
            }
        }
    }

    init {
        setOnItemClickListener { index, _ ->
            val item: PlayerLobby = if (index < list.size) {
                list[index]
            } else {
                PlayerLobbyManager[info]
            }
            item.tpThere(info.player)
        }

        setOnToolbarItemClickListener { index, _ ->
            if (!PlayerLobbyManager.isInOwnLobby(info))
                when (index) {
                    6 -> {
                        when (review) {
                            true -> currentLobby.cancelReviewFor(info)
                            null -> currentLobby.likeBy(info)
                            else -> {
                                currentLobby.cancelReviewFor(info)
                                currentLobby.likeBy(info)
                            }
                        }
                        refresh()
                    }
                    5 -> {
                        when (review) {
                            false -> currentLobby.cancelReviewFor(info)
                            null -> currentLobby.dislikeBy(info)
                            else -> {
                                currentLobby.cancelReviewFor(info)
                                currentLobby.dislikeBy(info)
                            }
                        }
                        refresh()
                    }
                }
        }
    }
}