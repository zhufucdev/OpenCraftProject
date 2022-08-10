package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.TextUtil.TextColor.AQUA
import com.zhufu.opencraft.TextUtil.TextColor.GREEN
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class BlockUI(
    plugin: Plugin,
    player: Player,
    private val root: BlockLockManager.GroupBlockInfo?,
    override val parentInventory: ClickableInventory
) : PageInventory<BlockUI.Adapter>(
    Language[player, "ui.blockTitle"].toInfoMessage(),
    { Adapter(player, root?.children, root != null) },
    36, plugin
), Backable {

    private val getter = getLangGetter(player.info())

    class Adapter(player: Player, root: List<BlockLockManager.BaseInfo>?, val showDelete: Boolean) :
        PageInventory.Adapter() {
        val blocks =
            root
                ?: ArrayList<BlockLockManager.BaseInfo>().apply {
                    val uuid = player.uniqueId.toString()
                    BlockLockManager.forEach {
                        if (it.owner == uuid) {
                            add(it)
                        }
                    }
                }
        private val getter = getLangGetter(player.info())
        override val hasToolbar: Boolean
            get() = true
        override val size: Int
            get() = blocks.size + 1

        private fun accessible(info: BlockLockManager.BaseInfo) = buildString {
            if (info.accessible.isEmpty()) {
                append(getter["ui.block.nobodyAccessible"])
            } else {
                info.accessible.forEach {
                    append(Bukkit.getOfflinePlayer(it).name)
                    append(',')
                }
                deleteCharAt(lastIndex)
            }
        }

        override fun getItem(index: Int, currentPage: Int): ItemStack {
            return if (index < blocks.size) {
                when (val info = blocks[index]) {
                    is BlockLockManager.BlockInfo -> ItemStack(Material.GRASS_BLOCK).apply {
                        itemMeta = itemMeta!!.apply {
                            val rename = TextUtil.formatLore(info.name)
                            displayName(rename.first().toSuccessMessage())
                            val newLore = ArrayList<Component>()
                            if (rename.size > 1) {
                                for (i in 1 until rename.size) {
                                    newLore.add(rename[i].toSuccessMessage())
                                }
                            }

                            val located =
                                TextUtil.formatLore(getter["ui.block.located", "${info.location.world!!.name},(${info.location.blockX},${info.location.blockY},${info.location.blockZ})"])
                            newLore.addAll(
                                listOf(
                                    getter["block.block"].toComponent().color(NamedTextColor.DARK_AQUA),
                                    getter["ui.block.accessible", accessible(info)].toInfoMessage()
                                )
                            )
                            located.forEach {
                                newLore.add(it.toInfoMessage())
                            }
                            newLore.add(getter["ui.block.click"].toTipMessage())
                            lore(newLore)
                        }
                    }

                    is BlockLockManager.GroupBlockInfo -> ItemStack(Material.CHEST).apply {
                        itemMeta = itemMeta!!.apply {
                            val rename = TextUtil.formatLore(info.name)
                            displayName(rename.first().toSuccessMessage())
                            val newLore = ArrayList<Component>()
                            if (rename.size > 1) {
                                for (i in 1 until rename.size) {
                                    newLore.add(rename[i].toSuccessMessage())
                                }
                            }

                            val children = TextUtil.formatLore(
                                getter[
                                        "ui.block.group.children",
                                        buildString {
                                            if (info.children.isEmpty()) {
                                                append(getter["ui.block.group.noChild"])
                                            } else {
                                                info.children.forEach {
                                                    append(it.name)
                                                    append(',')
                                                }
                                                deleteCharAt(lastIndex)
                                            }
                                        }
                                ]
                            )

                            newLore.addAll(
                                listOf(
                                    getter["block.group"].toComponent().color(NamedTextColor.DARK_AQUA),
                                    getter["ui.block.accessible", accessible(info)].toInfoMessage()
                                )
                            )
                            children.forEach {
                                newLore.add(it.toInfoMessage())
                            }
                            newLore.add(getter["ui.block.click"].toTipMessage())
                            lore(newLore)
                        }
                    }

                    else -> ItemStack(Material.BARRIER).apply {
                        itemMeta = itemMeta!!.apply {
                            displayName(getter["command.error.unknown"].toErrorMessage())
                        }
                    }
                }
            } else {
                Widgets.confirm.updateItemMeta<ItemMeta> {
                    displayName(getter["ui.block.new"].toSuccessMessage())
                }
            }
        }

        override fun getToolbarItem(index: Int): ItemStack {
            return if (index == 0 && showDelete) {
                Widgets.rename.apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.block.selecting.title"].toInfoMessage())
                        lore(
                            buildList {
                                add(getter["ui.block.selecting.tip.1"].toComponent().color(NamedTextColor.DARK_AQUA))
                                for (i in 2..6) {
                                    add(getter["ui.block.selecting.tip.$i"].toTipMessage())
                                }
                            }
                        )
                    }
                }
            } else if (index == 4 && !showDelete) ItemStack(Material.CHEST).apply {
                itemMeta = itemMeta!!.apply {
                    displayName(getter["ui.block.grouping.title"].toComponent().color(NamedTextColor.DARK_AQUA))
                    lore(TextUtil.formatLore(getter["ui.block.grouping.subtitle"]).map { it.toInfoMessage() })
                }
            }
            else if (index == 5 && showDelete)
                Widgets.close.apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.delete"].toErrorMessage())
                        lore(TextUtil.formatLore(getter["ui.block.deleteAlert"]).map { it.toWarnMessage() })
                    }
                }
            else if (index == 6) ItemStack(Material.ARROW).apply {
                itemMeta = itemMeta!!.apply {
                    displayName(getter["ui.back"].toInfoMessage())
                }
            }
            else super.getToolbarItem(index)
        }
    }

    init {
        setOnItemClickListener { index, item ->
            if (index < adapter.blocks.size) {
                val info = adapter.blocks[index]
                if (info is BlockLockManager.GroupBlockInfo) {
                    BlockUI(plugin, player, info, this).show(player)
                } else if (info is BlockLockManager.BlockInfo) {
                    BlockController(plugin, this, info, item, player).show(player)
                }
            } else {
                ServerCaller["NewBlock"]!!(listOf(player))
                close()
            }
        }
        setOnToolbarItemClickListener { index, _ ->
            when (index) {
                0 -> {
                    BlockLockManager.selected[player] = root!!
                    player.info(getter["ui.block.selecting.done", root.name])
                    close()
                }

                4 -> {
                    BlockGroupingUI(player, plugin, this).show(player)
                }

                5 -> {
                    val info = root!!
                    BlockLockManager.remove(info)
                    player.success(getter["block.delete", info.name, getter["block.group"]])
                    if (parentInventory is PageInventory<*>) parentInventory.refresh()
                    back(player)
                }

                6 -> back(player)
            }
        }
    }
}