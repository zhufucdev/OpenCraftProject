package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.TextUtil.TextColor.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.material.MaterialData
import org.bukkit.plugin.Plugin

class BlockUI(plugin: Plugin, player: Player, private val root: BlockLockManager.GroupBlockInfo?, override val parentInventory: ClickableInventory)
    : PageInventory<BlockUI.Adapter>(
        TextUtil.info(Language[player, "ui.blockTitle"]),
        { Adapter(player, root?.children, root != null) },
        36, plugin), PluginBase, Backable {

    private val getter = getLangGetter(player.info())

    class Adapter(player: Player, root: List<BlockLockManager.BaseInfo>?, val showDelete: Boolean) : PageInventory.Adapter(), PluginBase {
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
            get() = blocks.size

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
            val info = blocks[index]
            return when (info) {
                is BlockLockManager.BlockInfo -> ItemStack(Material.GRASS_BLOCK).apply {
                    itemMeta = itemMeta!!.apply {
                        val rename = TextUtil.formatLore(info.name)
                        setDisplayName(TextUtil.getColoredText(rename.first(), GREEN))
                        val newLore = ArrayList<String>()
                        if (rename.size > 1) {
                            for (i in 1 until rename.size) {
                                newLore.add(TextUtil.getColoredText(rename[i], GREEN))
                            }
                        }

                        val located = TextUtil.formatLore(getter["ui.block.located", "(${info.world},${info.from}-${info.to})"])
                        newLore.addAll(listOf(
                                TextUtil.getColoredText(getter["block.block"], AQUA),
                                TextUtil.info(getter["ui.block.accessible", accessible(info)])
                        ))
                        located.forEach {
                            newLore.add(TextUtil.info(it))
                        }
                        newLore.add(TextUtil.tip(getter["ui.block.click"]))
                        lore = newLore
                    }
                }
                is BlockLockManager.GroupBlockInfo -> ItemStack(Material.CHEST).apply {
                    itemMeta = itemMeta!!.apply {
                        val rename = TextUtil.formatLore(info.name)
                        setDisplayName(TextUtil.getColoredText(rename.first(), GREEN))
                        val newLore = ArrayList<String>()
                        if (rename.size > 1) {
                            for (i in 1 until rename.size) {
                                newLore.add(TextUtil.getColoredText(rename[i], GREEN))
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

                        newLore.addAll(listOf(
                                TextUtil.getColoredText(getter["block.group"], AQUA),
                                TextUtil.info(getter["ui.block.accessible", accessible(info)])
                        ))
                        children.forEach {
                            newLore.add(TextUtil.info(it))
                        }
                        newLore.add(TextUtil.tip(getter["ui.block.click"]))
                        lore = newLore
                    }
                }
                else -> ItemStack(Material.BARRIER).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.error(getter["command.error.unknown"]))
                    }
                }
            }
        }

        override fun getToolbarItem(index: Int): ItemStack {
            return if (index == 0 && showDelete) {
                Widgets.rename.apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.info(getter["ui.block.selecting.title"]))
                        val newLore = ArrayList<String>()
                        newLore.add(TextUtil.getColoredText(getter["ui.block.selecting.tip.1"], TextUtil.TextColor.AQUA))
                        for (i in 2..6) {
                            newLore.add(TextUtil.tip(getter["ui.block.selecting.tip.$i"]))
                        }
                        lore = newLore
                    }
                }
            } else if (index == 4 && !showDelete) ItemStack(Material.CHEST).apply {
                itemMeta = itemMeta!!.apply {
                    setDisplayName(TextUtil.getColoredText(getter["ui.block.grouping.title"], AQUA))
                    val newLore = ArrayList<String>()
                    TextUtil.formatLore(getter["ui.block.grouping.subtitle"]).forEach {
                        newLore.add(TextUtil.info(it))
                    }
                    lore = newLore
                }
            }
            else if (index == 5 && showDelete)
                Widgets.close.apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.getColoredText(getter["ui.delete"], TextUtil.TextColor.RED))
                        val newLore = ArrayList<String>()
                        TextUtil.formatLore(getter["ui.block.deleteAlert"]).forEach {
                            newLore.add(TextUtil.warn(it))
                        }
                        lore = newLore
                    }
                }
            else if (index == 6) ItemStack(Material.ARROW).apply {
                itemMeta = itemMeta!!.apply {
                    setDisplayName(TextUtil.info(getter["ui.back"]))
                }
            }
            else super.getToolbarItem(index)
        }
    }

    init {
        setOnItemClickListener { index, item ->
            val info = adapter.blocks[index]
            if (info is BlockLockManager.GroupBlockInfo) {
                BlockUI(plugin, player, info, this).show(player)
            } else if (info is BlockLockManager.BlockInfo) {
                BlockController(plugin, this, info, item, player).show(player)
            }
        }
        setOnToolbarItemClickListener { index, _ ->
            when (index) {
                0 -> {
                    BlockLockManager.selected[player] = root!!
                    player.info(getter["ui.block.selecting.done",root.name])
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