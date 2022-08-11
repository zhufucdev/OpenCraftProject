package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.util.*
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

class EditorUI(val project: TutorialManager.Tutorial)
    : PageInventory<EditorUI.Adapter>(
        "项目编辑器[项目ID:${project.id}]".toInfoMessage(),
        Adapter(project),
        4*9,
        TutorialManager.mPlugin!!) {
    class Adapter(private val project: TutorialManager.Tutorial): PageInventory.Adapter(){
        override val size: Int
            get() = project.size
        override val hasToolbar: Boolean = true

        override fun getItem(index: Int,currentPage: Int): ItemStack {
            val it = project[index]
            val stack = ItemStack(Material.ENDER_EYE)
            stack.itemMeta = stack.itemMeta!!.also { meta ->
                meta.setDisplayName(TextUtil.getColoredText("第${index+1}步", TextUtil.TextColor.AQUA, true, false))
                meta.lore = listOf(
                        "主标题: ${it.title}",
                        "副标题: ${it.subTitle}",
                        "时延: ${it.time}毫秒(${it.time / 1000.toDouble()}秒)",
                        if (it.type == TutorialManager.Tutorial.TutorialSwitchType.Teleport) "瞬间传送" else "平滑移动",
                        TextUtil.tip("点击进入记录模式以修改参数")
                )
            }
            return stack
        }

        override fun getToolbarItem(index: Int): ItemStack {
            return when (index){
                0 -> {
                    val add = Widgets.confirm
                    add.updateItemMeta<ItemMeta> {
                        displayName("添加步骤".toComponent().color(NamedTextColor.GREEN))
                        lore(listOf(
                            "这将会使您进入记录模式".toTipMessage(),
                            "您的聊天栏会被替换为记录模式指令输入器，输入\"help\"查看帮助".toTipMessage()
                        ))
                    }
                }
                1 -> {
                    ItemStack(Material.SNOWBALL).updateItemMeta<ItemMeta> {
                        displayName("预览".toSuccessMessage())
                    }
                }
                2 -> {
                    ItemStack(Material.LEVER).updateItemMeta<ItemMeta> {
                        displayName("触发方式".toSuccessMessage())
                        lore(listOf("点击查看".toTipMessage()))
                    }
                }
                6 -> {
                    Widgets.cancel.updateItemMeta<ItemMeta> {
                        displayName("删除".toErrorMessage())
                    }
                }
                else -> super.getToolbarItem(index)
            }
        }
    }

    init {
        setOnItemClickListener { index, _ ->
            close()
            TutorialListener.instance.recorder(Bukkit.getPlayer(UUID.fromString(project.creator))!!,project[index])
        }
        setOnToolbarItemClickListener { index, _ ->
            when (index){
                0 -> {
                    close()
                    TutorialListener.instance.recorder(Bukkit.getPlayer(UUID.fromString(project.creator))!!,project.addNewStep())
                }
                1 -> {
                    close()
                    project.play(Bukkit.getPlayer(showingTo!!.uniqueId)!!)
                }
                2 -> {
                    close()
                    TriggerUI(plugin).show(showingTo!!)
                }
                6 -> {
                    close()
                    TutorialListener.instance.removeCreator(Bukkit.getPlayer(showingTo!!.uniqueId)!!)
                    TutorialManager.del(project.id)
                    showingTo!!.sendMessage("已删除该教程".toInfoMessage())
                }
            }
        }
    }

}