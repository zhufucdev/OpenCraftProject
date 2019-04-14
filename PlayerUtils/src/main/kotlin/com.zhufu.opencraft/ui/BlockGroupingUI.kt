package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

class BlockGroupingUI(private val player: Player, plugin: Plugin, override val parentInventory: ClickableInventory)
    : PageInventory<BlockGroupingUI.Adapter>(TextUtil.info(Language[player, "ui.block.grouping.title"]),
        Adapter(player), 36, plugin), Backable, PluginBase {

    private val selected
        get() = adapter.selected

    class Adapter(private val player: Player) : PageInventory.Adapter(), PluginBase {
        var blocks: ArrayList<BlockLockManager.BaseInfo> = ArrayList()

        init {
            initBlocks(false)
        }

        fun initBlocks(groupChoosingMode: Boolean) {
            this.groupChoosingMode = groupChoosingMode
            blocks.apply {
                clear()
                val uuid = player.uniqueId.toString()
                BlockLockManager.forEach {
                    if (it.owner == uuid && (!groupChoosingMode && it is BlockLockManager.BlockInfo) || (groupChoosingMode && it is BlockLockManager.GroupBlockInfo)) {
                        add(it)
                    }
                }
            }
        }

        var groupChoosingMode = false
        val selected = ArrayList<BlockLockManager.BlockInfo>()
        val getter = getLangGetter(player.info())
        override val hasToolbar: Boolean
            get() = true
        override val size: Int
            get() = blocks.size + if (groupChoosingMode) 1 else 0

        override fun getItem(index: Int, currentPage: Int): ItemStack {
            return if (!groupChoosingMode) {
                val info = blocks[index]
                val selected = selected.contains(info)
                ItemStack(if (selected) Material.GRASS_BLOCK else Material.GRASS_PATH).apply {
                    itemMeta = itemMeta!!.apply {
                        val rename = TextUtil.formatLore(info.name)
                        setDisplayName(TextUtil.getColoredText(rename.first(), TextUtil.TextColor.GREEN))
                        val newLore = ArrayList<String>()
                        for (i in 1 until rename.size) {
                            newLore.add(TextUtil.getColoredText(rename[i], TextUtil.TextColor.GREEN))
                        }

                        if (!selected) {
                            newLore.add(TextUtil.tip(getter["ui.block.grouping.select"]))
                        } else {
                            newLore.add(TextUtil.tip(getter["ui.block.grouping.cancel"]))
                            addUnsafeEnchantment(Enchantment.DURABILITY, 1)
                        }
                        lore = newLore
                    }
                }
            } else {
                if (index < blocks.size) {
                    val info = blocks[index]
                    Widgets.group.apply {
                        itemMeta = itemMeta!!.apply {
                            val rename = TextUtil.formatLore(info.name)
                            setDisplayName(TextUtil.getColoredText(rename.first(), TextUtil.TextColor.GREEN))
                            val newLore = ArrayList<String>()
                            for (i in 1 until rename.size) {
                                newLore.add(TextUtil.getColoredText(rename[i], TextUtil.TextColor.GREEN))
                            }
                            newLore.add(TextUtil.tip(getter["ui.block.grouping.join"]))

                            lore = newLore
                        }
                    }
                } else {
                    Widgets.confirm.apply {
                        itemMeta = itemMeta!!.apply {
                            setDisplayName(TextUtil.info(getter["ui.block.group.new"]))
                        }
                    }
                }
            }
        }

        override fun getToolbarItem(index: Int): ItemStack {
            return when (index) {
                5 -> if (!groupChoosingMode)
                    Widgets.confirm.apply {
                        itemMeta = itemMeta!!.apply {
                            setDisplayName(TextUtil.getColoredText(getter["ui.confirm"], TextUtil.TextColor.GREEN))
                        }
                    }
                else
                    super.getToolbarItem(index)
                6 -> Widgets.back.apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.info(getter["ui.cancel"]))
                    }
                }
                else -> super.getToolbarItem(index)
            }
        }
    }

    init {
        setOnItemClickListener { index, _ ->
            if (!adapter.groupChoosingMode) {
                val info = adapter.blocks[index] as BlockLockManager.BlockInfo
                if (selected.contains(info)) {
                    selected.remove(info)
                } else {
                    selected.add(info)
                }
                refresh(index)
            } else {
                //To merge
                val new: Boolean
                val info = if (index < adapter.blocks.size) {
                    new = false
                    adapter.blocks[index] as BlockLockManager.GroupBlockInfo
                }
                else {
                    new = true
                    val prefix = adapter.getter["ui.block.group.new"]
                    var max = 0
                    BlockLockManager.forEach {
                        if (it.name.startsWith(prefix)){
                            it.name.substring(prefix.length).toIntOrNull()?.apply {
                                if (this > max)
                                    max = this
                            }
                        }
                    }
                    val name = prefix + (max+1).toString()
                    player.info(adapter.getter["block.grouping.createAs",name])
                    BlockLockManager.GroupBlockInfo(name).apply {
                        owner = player.uniqueId.toString()
                    }
                }

                var s = 0
                selected.forEach {
                    if (!info.contains(it)) {
                        BlockLockManager.remove(it)
                        info.add(it)
                        if (new){
                            BlockLockManager.add(info)
                        }
                        s++
                    }
                }

                player.success(getLang(player, "ui.block.grouping.done", s, info.name))
                (parentInventory as PageInventory<*>).refresh()
                if (new){
                    player.tip(adapter.getter["ui.block.renameTip"])
                    BlockLockManager.selected[player] = info
                }
                back(player)
            }
        }
        setOnToolbarItemClickListener { index, _ ->
            when (index) {
                5 -> {
                    adapter.initBlocks(true)
                    refresh()
                }
                6 -> back(player)
            }
        }
    }
}