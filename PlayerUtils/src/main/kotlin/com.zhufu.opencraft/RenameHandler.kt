package com.zhufu.opencraft

import com.zhufu.opencraft.api.Nameable
import org.bukkit.entity.Player

object RenameHandler {
    private val selection = hashMapOf<Player, Selection>()

    fun select(player: Player, nameable: Nameable): SelectionCallback {
        val s = Selection(nameable)
        selection[player] = s
        return s
    }

    fun hasSelection(player: Player) = selection.containsKey(player)

    fun rename(player: Player, newName: String) {
        val selected = selection[player] ?: return
        val info = player.info()
        val getter = info.getter()
        if (info == null) {
            player.error(getter["player.error.unknown"])
        } else {
            val oldName = selected.item.name.takeIf { it.isNotEmpty() } ?: getter["command.unnamed"]
            selected.item.name = newName
            player.success(getter["ui.renamed", oldName, newName])
            selected.postRename?.invoke()
            selection.remove(player)
        }
    }

    private class Selection(val item: Nameable) : SelectionCallback {
        var postRename: (() -> Unit)? = null

        override fun setPostRenameListener(l: () -> Unit) {
            postRename = l
        }
    }
}

interface SelectionCallback {
    fun setPostRenameListener(l: () -> Unit)
}