package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.util.toComponent
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toTipMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

class TutorialExp(val player: Player) : PageInventory<TutorialExp.Adapter>(
    "教程浏览器[uuid:${++id}]".toInfoMessage(),
    Adapter(player),
    4 * 9,
    TutorialManager.mPlugin!!
) {
    companion object {
        var id = 0
    }

    class Adapter(val player: Player) : PageInventory.Adapter() {
        val tutorialList = TutorialManager.everything()
        override val size: Int
            get() = tutorialList.size + 1

        override fun getItem(index: Int, currentPage: Int): ItemStack {
            return if (index < tutorialList.size) {
                val r = ItemStack(Material.ENDER_EYE)
                val t = tutorialList[index]
                r.editMeta {
                    it.displayName(t.name.toInfoMessage())
                    it.lore(
                        listOf(
                            (if (t.isDraft) "草稿" else "成品").toInfoMessage(),
                            "共有${t.size}个步骤".toInfoMessage(),
                            (Bukkit.getOfflinePlayer(UUID.fromString(t.creator)).name + "原著").toComponent(),
                            (if (t.creator == player.uniqueId.toString()) "点击修改" else "点击播放").toTipMessage()
                        )
                    )
                }
                r
            } else {
                val r = Widgets.confirm
                r.itemMeta = r.itemMeta!!.also {
                    it.displayName("新建".toInfoMessage())
                }
                r
            }
        }
    }

    init {
        setOnItemClickListener { index, _ ->
            val e = this.adapter.tutorialList
            if (index == e.size) {
                val r = TutorialManager.Tutorial(player.uniqueId.toString(), "未命名")
                TutorialManager.addAsDraft(r)
                TutorialListener.instance.creator(player, r)
            } else {
                val t = e[index]
                if (t.creator == player.uniqueId.toString()) {
                    TutorialListener.instance.creator(player, t)
                } else {
                    close()
                    t.play(player, true)
                }
            }
        }
    }

    fun show() {
        super.show(player)
    }
}