package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.inventory.PaymentDialog
import com.zhufu.opencraft.Base.Extend.toPrettyString
import com.zhufu.opencraft.data.Checkpoint
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class CheckpointUI(val info: Info, plugin: Plugin, override val parentInventory: IntractableInventory) :
    PageInventory<CheckpointUI.Adapter>(
        Language[info.userLanguage, "ui.checkpoint.title"].toComponent(),
        Adapter(info, info.getter()), 36, plugin
    ), Backable {
    private val tasks
        get() = adapter.tasks

    class Adapter(val info: Info, val getter: Language.LangGetter) :
        PageInventory.Adapter() {
        var checkpoints = info.checkpoints.sortedBy { it.name }
        override val size: Int
            get() = checkpoints.size + if (!isManaging) 1 else 0
        override val hasToolbar: Boolean
            get() = true
        val tasks = HashMap<Checkpoint, Char>()
        var isManaging = false
        var isDeleting = false
        var isRenaming = false
        fun reset() {
            isManaging = false
            isRenaming = false
            isDeleting = false
        }

        override fun getItem(index: Int, currentPage: Int): ItemStack {
            return if (index < checkpoints.size) {
                val info = checkpoints[index]
                val selected = tasks.containsKey(info)
                ItemStack(if (!selected) Material.ENDER_PEARL else Material.ENDER_EYE).apply {
                    itemMeta = itemMeta!!.apply {
                        val rename = TextUtil.formatLore(info.name)
                        displayName(rename.first().toComponent().color(NamedTextColor.AQUA))
                        val newLore = ArrayList<Component>()
                        for (i in 1 until rename.size) {
                            newLore.add(rename[i].toComponent().color(NamedTextColor.AQUA))
                        }
                        newLore.add(
                            getter[
                                    if (!isManaging)
                                        "ui.checkpoint.click"
                                    else {
                                        if (isDeleting) {
                                            if (tasks[info] == 'D')
                                                "ui.checkpoint.delete.undo"
                                            else
                                                "ui.checkpoint.delete.do"
                                        } else if (isRenaming) {
                                            "ui.checkpoint.rename.do"
                                        } else "ui.checkpoint.toManage"
                                    }
                            ].toTipMessage()
                        )

                        lore(newLore)
                    }
                }
            } else {
                Widgets.confirm.apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(
                            getter["ui.checkpoint.new.title"].toSuccessMessage()
                        )
                        lore(listOf(
                            getter["ui.checkpoint.new.subtitle"].toInfoMessage()
                        ))
                    }
                }
            }
        }

        override fun getToolbarItem(index: Int): ItemStack {
            return when (index) {
                3 -> {
                    if (isManaging && !isDeleting && !isRenaming) {
                        Widgets.rename.apply {
                            itemMeta = itemMeta!!.apply {
                                displayName(getter["ui.rename"].toTipMessage())
                            }
                        }
                    } else
                        super.getToolbarItem(index)
                }

                4 -> {
                    if (isManaging && !isDeleting && !isRenaming) {
                        Widgets.cancel.apply {
                            itemMeta = itemMeta!!.apply {
                                displayName(getter["ui.delete"].toErrorMessage())
                            }
                        }
                    } else
                        super.getToolbarItem(index)
                }

                5 -> {
                    if (!isManaging)
                        Widgets.group.apply {
                            itemMeta = itemMeta!!.apply {
                                displayName(getter["ui.manage"].toTipMessage())
                            }
                        }
                    else
                        Widgets.confirm.apply {
                            itemMeta = itemMeta!!.apply {
                                displayName(getter["ui.confirm"].toSuccessMessage())
                            }
                        }
                }

                6 -> {
                    Widgets.back.apply {
                        itemMeta = itemMeta!!.apply {
                            displayName(getter["ui.back"].toTipMessage())
                        }
                    }
                }

                else -> super.getToolbarItem(index)
            }
        }

        override fun onRefresh() {
            checkpoints = info.checkpoints.sortedBy { it.name }
        }
    }

    init {
        setOnItemClickListener { index, _ ->
            if (index < adapter.checkpoints.size) {
                val point = adapter.checkpoints[index]
                if (!adapter.isManaging) {
                    PaymentDialog(
                        info.player,
                        SellingItemInfo(
                            ItemStack(Material.ENDER_PEARL)
                                .updateItemMeta<ItemMeta> {
                                    displayName(adapter.getter["ui.teleport"].toInfoMessage())
                                },
                            3,
                            1
                        ), TradeManager.getNewID(), plugin
                    )
                        .setOnPayListener { success ->
                            if (success) {
                                val event = PlayerTeleportedEvent(info.player, info.player.location, point.location)
                                Bukkit.getPluginManager().callEvent(event)
                                if (!event.isCancelled) {
                                    info.player.teleport(point.location)
                                    info.player.info(adapter.getter["user.checkpoint.tpSucceed"])
                                }
                            } else {
                                info.player.error(info.getter()["trade.error.poor"])
                            }
                            true
                        }
                        .setOnCancelListener {
                            info.player.info(adapter.getter["user.teleport.cancelled"])
                            show(info.player)
                        }
                        .show()
                } else {
                    if (adapter.isDeleting) {
                        if (tasks[point] == 'D') {
                            tasks.remove(point)
                        } else {
                            tasks[point] = 'D'
                        }
                        refresh(index)
                    } else if (adapter.isRenaming) {
                        PlayerUtil.selected[info.player] = adapter.checkpoints[index]
                        info.player.tip(adapter.getter["ui.checkpoint.rename.tip"])
                        close()
                    }
                }
            } else {
                val prefix = adapter.getter["ui.checkpoint.new.title"]
                var max = 0
                adapter.checkpoints.forEach {
                    if (it.name.startsWith(prefix)) {
                        it.name.substring(prefix.length).toIntOrNull()?.apply {
                            if (this > max)
                                max = this
                        }
                    }
                }
                val name = prefix + (max + 1).toString()
                info.player.apply {
                    if (location.world == Base.lobby) {
                        error(this@CheckpointUI.adapter.getter["command.error.world"])
                    } else {
                        info.checkpoints.add(Checkpoint(info.player.location, name))
                        success(
                            adapter.getter[
                                    "user.checkpoint.saved",
                                    location.toPrettyString(),
                                    name
                            ]
                        )
                        refresh(index)
                        refresh(index + 1)
                    }
                }
            }
        }

        setOnToolbarItemClickListener { index, _ ->
            when (index) {
                3 -> {
                    if (adapter.isManaging && !adapter.isRenaming) {
                        adapter.isRenaming = true
                        refresh()
                    }
                }

                4 -> {
                    if (adapter.isManaging && !adapter.isDeleting) {
                        adapter.isDeleting = true
                        refresh()
                    }
                }

                5 -> {
                    if (!adapter.isManaging) {
                        adapter.isManaging = true
                        refresh()
                    } else {
                        //Apply

                        tasks.forEach { (t, u) ->
                            if (u == 'D') {
                                info.removeCheckpoint(t.name)
                            }
                        }
                        adapter.reset()
                        refresh()
                        if (tasks.isNotEmpty())
                            info.player.success(adapter.getter["ui.checkpoint.delete.done", tasks.size])
                        tasks.clear()
                    }
                }

                6 -> {
                    if (!adapter.isManaging) {
                        back(info.player)
                    } else {
                        adapter.reset()
                        tasks.clear()
                        refresh()
                    }
                }
            }
        }
    }
}