package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.material.MaterialData
import java.util.*

class TutorialExp(val player: Player)
    : PageInventory<TutorialExp.Adapter>(
        TextUtil.info("教程浏览器[uuid:${++id}]"),
        Adapter(player),
        4*9,
        TutorialManager.mPlugin!!) {
    companion object {
        var id = 0
    }

    class Adapter(val player: Player): PageInventory.Adapter(){
        val tutorialList = TutorialManager.everything()
        override val size: Int
            get() = tutorialList.size + 1
        override fun getItem(index: Int,currentPage: Int): ItemStack {
            return if (index < tutorialList.size) {
                val r = ItemStack(Material.ENDER_EYE)
                val t = tutorialList[index]
                r.itemMeta = r.itemMeta!!.also {
                    it.setDisplayName( TextUtil.getColoredText(t.name, TextUtil.TextColor.GOLD, false, false))
                    it.lore = listOf(
                            TextUtil.info(
                                    if (t.isDraft)"草稿" else "成品"
                            ),
                            TextUtil.info("共有${t.size}个步骤"),
                            Bukkit.getOfflinePlayer(UUID.fromString(t.creator))?.name + "原著",
                            TextUtil.tip(
                                    if (t.creator == player.uniqueId.toString()) "点击修改" else "点击播放"
                            )
                    )
                }
                r
            } else {
                val r = Widgets.confirm
                r.itemMeta = r.itemMeta!!.also {
                    it.setDisplayName(TextUtil.tip("新建"))
                }
                r
            }
        }
    }

    init {
        setOnItemClickListener { index, item ->
            val e = (this.adapter as Adapter).tutorialList
            if (index == e.size){
                val r = TutorialManager.Tutorial(player.uniqueId.toString(),"未命名")
                TutorialManager.addAsDraft(r)
                TutorialListener.mInstance.creator(player,r)
            }
            else{
                val t = e[index]
                if (t.creator == player.uniqueId.toString()){
                    TutorialListener.mInstance.creator(player,t)
                }else{
                    close()
                    t.play(player,true)
                }
            }
        }
    }

    fun show(){
        super.show(player)
    }
}