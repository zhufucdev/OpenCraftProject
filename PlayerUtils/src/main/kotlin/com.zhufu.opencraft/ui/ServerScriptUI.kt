package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.script.PlayerScript
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class ServerScriptUI(private val who: Info, plugin: Plugin, private val parent: ClickableInventory) :
    PageInventory<ServerScriptUI.SSAdapter>(getLang(who, "scripting.ui.title"), SSAdapter(who), 36, plugin) {
    private val list get() = adapter.list

    class SSAdapter(who: ServerPlayer) : PageInventory.Adapter() {
        var list = PlayerScript.list(who)
        val getter = Language.LangGetter(who.userLanguage)
        var mode = 0 // 0 -> Executing, 1 -> Renaming, 2 -> Deleting, 3 -> Editing

        override val size: Int
            get() = list.size + if (mode == 0) 1 else 0

        override fun getItem(index: Int, currentPage: Int): ItemStack =
            if (index < list.size)
                ItemStack(Material.PAPER).updateItemMeta<ItemMeta> {
                    setDisplayName(list[index].name)
                    lore = listOf(
                        getter[
                                when (mode) {
                                    0 -> "scripting.ui.clickToExec"
                                    1 -> "ui.clickToRename"
                                    2 -> "ui.clickToDelete"
                                    else -> "ui.others.cb.edit.click"
                                }
                        ].toTipMessage()
                    )
                }
            else
                Widgets.confirm.updateItemMeta<ItemMeta> {
                    setDisplayName(getter["scripting.ui.new"].toSuccessMessage())
                }

        override val hasToolbar: Boolean
            get() = true

        override fun getToolbarItem(index: Int): ItemStack {
            return when (index) {
                6 -> Widgets.back.updateItemMeta<ItemMeta> {
                    setDisplayName(getter["ui.back"])
                }
                5 -> {
                    if (mode == 0)
                        Widgets.cancel.updateItemMeta<ItemMeta> {
                            setDisplayName(getter["ui.delete"].toErrorMessage())
                        }
                    else
                        super.getToolbarItem(index)
                }
                4 -> {
                    if (mode == 0)
                        Widgets.rename.updateItemMeta<ItemMeta> {
                            setDisplayName(getter["ui.rename"].toInfoMessage())
                        }
                    else
                        super.getToolbarItem(index)
                }
                3 -> {
                    if (mode == 0)
                        ItemStack(Material.WRITABLE_BOOK).updateItemMeta<ItemMeta> {
                            setDisplayName(getter["ui.others.cb.edit.title"].toInfoMessage())
                        }
                    else
                        super.getToolbarItem(index)
                }
                else -> super.getToolbarItem(index)
            }
        }
    }

    init {
        setOnItemClickListener { index, _ ->
            when (adapter.mode){
                0 -> {
                    close()
                    if (index < list.size) {
                        val script = list[index]
                        if (script.isClosed)
                            script.reset()
                        script.call()
                        script.close()
                    } else {
                        who.player.sendMessage(adapter.getter["scripting.ui.tip.editBook"].toTipMessage())
                        Everything.openBookEditFor(who.player) {
                            if (it != null) {
                                list.add(
                                    PlayerScript(
                                        player = who,
                                        src = it
                                    ).apply {
                                        // Renaming
                                        val filter = list.filter { item -> item.name.startsWith(this.name) }
                                        var max = 0
                                        for (item in filter) {
                                            val order = item.name.substring(this.name.length).toIntOrNull() ?: continue
                                            if (order > max) {
                                                max = order
                                            }
                                        }
                                        this.name += (max + 1).toString()
                                        write()

                                        PlayerUtil.selected[who.player] = this
                                    }
                                )
                                refresh()
                                who.player.tip(adapter.getter["ui.others.cb.rename.tip"])
                            }
                            Bukkit.getScheduler().runTaskLater(plugin,{ _ ->
                                show(who.player)
                            },5)
                        }
                    }
                }
                1 -> {
                    // Renaming
                    close()
                    PlayerUtil.selected[who.player] = list[index]
                    who.player.tip(adapter.getter["ui.others.cb.rename.tip"])
                }
                2 -> {
                    // Deleting
                    list.removeAt(index).srcFile?.delete()
                    refresh()
                }
                3 -> {
                    // Editing
                    close()
                    val item = list[index]
                    who.player.sendMessage(adapter.getter["scripting.ui.tip.editBook"].toTipMessage())
                    Everything.openBookEditFor(who.player,item.src) {
                        if (it != null) {
                            item.src = it
                            item.write()
                            who.player.success(adapter.getter["command.done"])
                        }
                        Bukkit.getScheduler().runTaskLater(plugin,{ _ ->
                            show(who.player)
                        },5)
                    }
                }
            }
        }
        setOnToolbarItemClickListener { index, _ ->
            when (index) {
                6 -> {
                    if (adapter.mode == 0)
                        parent.show(showingTo!!)
                    else {
                        adapter.mode = 0
                        refresh()
                    }
                }
                5 -> {
                    // Deleting
                    adapter.mode = 2
                    refresh()
                }
                4 -> {
                    // Renaming
                    adapter.mode = 1
                    refresh()
                }
                3 -> {
                    // Editing
                    adapter.mode = 3
                    refresh()
                }
            }
        }
    }
}