package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.task.LinearTask
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin
import java.text.DecimalFormat
import java.util.*

class TaskSelectUI(owner: Info, plugin: Plugin) : PageInventory<TaskSelectUI.Adapter>(
    title = owner.getter()["rpg.ui.task.title"].toInfoMessage(),
    adapter = Adapter(owner),
    itemsOnePage = 18,
    plugin = plugin
) {
    class Adapter(private val owner: Info) : PageInventory.Adapter() {
        val getter = owner.getter()
        override val size: Int
            get() = 43

        val tasksDone get() = TaskManager[owner].filterIsInstance<LinearTask>()
        var localTasks: List<LinearTask> = tasksDone
        private fun fetchTasksDone() {
            localTasks = tasksDone
        }

        override fun onRefresh() {
            fetchTasksDone()
        }

        private fun material(completion: Float): Material = when ((completion * 10).toInt()) {
            in 0 until 2 -> Material.BROWN_DYE
            in 2 until 4 -> Material.YELLOW_DYE
            in 4 until 8 -> Material.GREEN_DYE
            else -> Material.BLUE_DYE
        }

        override fun getItem(index: Int, currentPage: Int): ItemStack {
            val find = localTasks.firstOrNull { it.level == index + 1 }
            return if (find != null) {
                val status = TaskManager.status(find)
                val lore = arrayListOf<String>()
                fun timeFormats(milli: Long): Array<Any> {
                    val time = Calendar.getInstance()
                    time.timeInMillis = milli
                    return arrayOf(
                        time.get(Calendar.YEAR),
                        time.get(Calendar.MONTH),
                        time.get(Calendar.DAY_OF_MONTH),
                        time.get(Calendar.HOUR_OF_DAY),
                        time.get(Calendar.MINUTE)
                    )
                }
                lore.add(getter.get("rpg.ui.task.creation", *timeFormats(status.timeCreated)).toInfoMessage())
                if (find.isCompleted) {
                    lore.add(
                        getter.get("rpg.ui.task.completion", *timeFormats(status.timeDone)).toInfoMessage()
                    )
                }
                lore.add(getter["rpg.ui.task.degree", DecimalFormat("##.##").format(find.completeDegree * 100) + "%"])
                ItemStack(material(find.completeDegree), index + 1).updateItemMeta<ItemMeta> {
                    this.lore = lore
                    setDisplayName(getter["rpg.ui.task.name", index + 1])
                }
            } else {
                ItemStack(Material.GRAY_DYE).updateItemMeta<ItemMeta> {
                    lore = listOf(getter["rpg.ui.task.imcompletion"].toInfoMessage())
                    setDisplayName(getter["rpg.ui.task.name", index + 1])
                    addEnchant(Enchantment.ARROW_INFINITE, 1, true)
                    addItemFlags(ItemFlag.HIDE_ENCHANTS)
                }
            }
        }

    }

    init {
        setOnItemClickListener { index, _ ->
            val task = adapter.localTasks.firstOrNull { it.level == index + 1 } ?: LinearTask(owner, index + 1).also { TaskManager.add(it) }
            val player = Bukkit.getPlayer(showingTo!!.uniqueId)!!
            player.info(adapter.getter["rpg.ui.task.init.wait"].toInfoMessage())
            TaskManager.join(player, task)
            close()
        }
    }
}