package com.zhufu.opencraft

import com.zhufu.opencraft.rpg.Role.*
import com.zhufu.opencraft.special_item.base.SpecialItem
import org.bukkit.entity.Player

object RPGUtil {
    fun sendEquipmentsTo(player: Player) {
        val info = player.info()
        val getter = getLangGetter(info)
        if (info == null) {
            player.error(getter["player.error.unknown"])
            return
        }

        when (info.role) {
            MAGICIAN -> {
                val wand = SpecialItem.make("Wand", 1, player)!!
                val book = SpecialItem.make("MagicBook", 1, player)!!
                player.inventory.addItem(wand.itemLocation.itemStack, book.itemLocation.itemStack)
                info.tag.set("mp", 40)
            }
            RANGER -> TODO()
            PRIEST -> TODO()
            SUMMONER -> TODO()
            else -> {}
        }
    }
}

var ServerPlayer.level: Int
    get() = tag.getInt("level", 1)
    set(value) = tag.set("level", value)
