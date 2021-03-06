package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.Game.env
import com.zhufu.opencraft.TextUtil.TextColor.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.Plugin
import java.math.BigDecimal
import java.math.MathContext

class MenuInterface(plugin: Plugin, private val player: Player) :
    ClickableInventory(plugin) {
    companion object {
        var id = 0
    }

    private val info = player.info()
    private val getter = getLangGetter(info)
    override val inventory: Inventory =
        Bukkit.createInventory(null, 36, TextUtil.info(getter["ui.puTitle"] + "[${++id}]"))

    init {
        inventory.apply {
            setItem(
                0,
                ItemStack(Material.PAPER).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.getColoredText(getter["ui.statics.title"], AQUA))
                    }
                }
            )
            setItem(
                1,
                ItemStack(Material.PLAYER_HEAD).apply {
                    itemMeta = (itemMeta as SkullMeta).apply {
                        owningPlayer = Bukkit.getOfflinePlayer(player.uniqueId)
                        setDisplayName(TextUtil.getColoredText(getter["ui.statics.face.title", player.name], GREEN))
                        val chart = info?.let { Game.dailyChart.indexOf(it) } ?: -1
                        lore = listOf(
                            getter["ui.statics.face.tip"].toTipMessage(),
                            (if (chart != -1)
                                getter["ui.statics.face.chart", chart + 1]
                            else
                                getter["ui.statics.face.noChart"]).toInfoMessage()
                        )
                    }
                }
            )
            setItem(
                3,
                ItemStack(Material.CLOCK).apply {
                    itemMeta = itemMeta!!.apply {
                        val rename = TextUtil.formatLore(TextUtil.format(info?.gameTime ?: 0, getter))
                        setDisplayName(TextUtil.info(rename.first()))
                        val newLore = ArrayList<String>()
                        for (i in 1 until rename.size) {
                            newLore.add(TextUtil.info(rename[i]))
                        }
                        lore = newLore
                    }
                }
            )
            setItem(
                5,
                ItemStack(Material.GOLD_INGOT).apply {
                    itemMeta = itemMeta!!.apply {
                        val currency = info?.currency ?: -1
                        val rename = TextUtil.formatLore(
                            getter[
                                    "ui.statics.gold",
                                    currency,
                                    BigDecimal(currency.toDouble() / env.getInt("diamondExchange"))
                                        .round(MathContext(3))
                            ]
                        )
                        setDisplayName(TextUtil.info(rename.first()))
                        val newLore = ArrayList<String>()
                        for (i in 1 until rename.size) {
                            newLore.add(TextUtil.info(rename[i]))
                        }
                        lore = newLore
                    }
                }
            )
            setItem(
                8,
                ItemStack(Material.BARRIER).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.getColoredText(getter["ui.close"], RED))
                    }
                }
            )

            setItem(
                9,
                ItemStack(Material.PAPER).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.getColoredText(getter["ui.miniGame.title"], AQUA))
                    }
                }
            )
            setItem(
                10,
                ItemStack(Material.IRON_PICKAXE).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.getColoredText(getter["ui.miniGame.cw.title"], RED))
                        val rename = TextUtil.formatLore(getter["ui.miniGame.cw.subtitle"])
                        val newLore = ArrayList<String>()
                        rename.forEach {
                            newLore.add(TextUtil.getColoredText(it, AQUA))
                        }
                        addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        lore = newLore
                    }
                }
            )
            setItem(
                11,
                ItemStack(Material.DIAMOND_SWORD).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.getColoredText(getter["ui.miniGame.tms.title"], RED))
                        val rename = TextUtil.formatLore(getter["ui.miniGame.tms.subtitle"])
                        val newLore = ArrayList<String>()
                        rename.forEach {
                            newLore.add(TextUtil.getColoredText(it, AQUA))
                        }
                        addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        lore = newLore
                    }
                }
            )
            setItem(
                12,
                ItemStack(Material.PAPER).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.success(getter["ui.miniGame.paper"]))
                    }
                }
            )
            setItem(
                18,
                ItemStack(Material.PAPER).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.getColoredText(getter["ui.survival.title"], AQUA))
                    }
                }
            )
            setItem(
                19,
                ItemStack(Material.CHEST).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.getColoredText(getter["ui.survival.grass.title"], GREEN))
                        var count = 0
                        val uuid = player.uniqueId.toString()
                        BlockLockManager.forEach {
                            if (it.owner == uuid)
                                count++
                        }
                        lore = listOf(
                            TextUtil.info(getter["ui.survival.grass.subtitle", count]),
                            TextUtil.tip(getter["ui.survival.grass.click"])
                        )
                    }
                }
            )
            setItem(
                20,
                ItemStack(Material.ENDER_PEARL).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.getColoredText(getter["ui.survival.pearl.title"], AQUA))
                        lore = listOf(
                            TextUtil.info(getter["ui.survival.pearl.subtitle"]),
                            TextUtil.tip(getter["ui.survival.pearl.click"])
                        )
                    }
                }
            )
            setItem(
                21,
                ItemStack(Material.GRASS_BLOCK).updateItemMeta<ItemMeta> {
                    setDisplayName(getter["ui.world.title"].toSuccessMessage())
                    lore = listOf(getter["ui.world.click"].toTipMessage())
                }
            )
            setItem(
                22,
                ItemStack(Material.PLAYER_HEAD).updateItemMeta<ItemMeta> {
                    setDisplayName(TextUtil.getColoredText(getter["ui.friend.title"], AQUA, bold = true))
                    lore = listOf(getter["ui.friend.check"].toTipMessage())
                }
            )
            setItem(
                27,
                ItemStack(Material.PAPER).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.getColoredText(getter["ui.others.title"], AQUA))
                    }
                }
            )
            setItem(
                28,
                ItemStack(Material.SNOWBALL).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.getColoredText(getter["ui.others.snowball.title"], AQUA))
                        lore = listOf(
                            TextUtil.info(getter["ui.others.snowball.subtitle"]),
                            TextUtil.tip(getter["ui.others.snowball.click"])
                        )
                    }
                }
            )
            setItem(
                29,
                ItemStack(Material.ARMOR_STAND).apply {
                    itemMeta = itemMeta!!.apply {
                        setDisplayName(TextUtil.getColoredText(getter["ui.others.armor.title"], AQUA))
                        lore = listOf(
                            TextUtil.info(getter["ui.others.armor.subtitle", info?.skin ?: player.name]),
                            TextUtil.tip(getter["ui.others.armor.click"])
                        )
                    }
                }
            )
        }
    }

    override fun onClick(event: InventoryClickEvent) {
        when (event.rawSlot) {
            1 -> {
                Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
                    val info = player.info()
                    if (info != null) {
                        player.sendActionText(getter["ui.chart.booting"].toInfoMessage())
                        val ui = ChartUI(plugin, info, this)
                        Bukkit.getScheduler().callSyncMethod(plugin) {
                            ui.show(info.player)
                        }
                    }
                }
                close()
            }
            8 -> close()
            10 -> GameManager.joinPlayerCorrectly(player, "CW")
            11 -> GameManager.joinPlayerCorrectly(player, "TMS")
            19 -> BlockUI(plugin, player, null, this).show(player)
            20 -> {
                val info = player.info()
                if (info == null) {
                    close()
                } else {
                    CheckpointUI(info, plugin, this).show(player)
                }
            }
            21 -> {
                val info = player.info()
                if (info == null) {
                    close()
                } else {
                    WorldUI(plugin, info, this).show(player)
                }
            }
            22 -> {
                val info = player.info()
                if (info == null) {
                    close()
                } else {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
                        player.sendActionText(getter["ui.friend.booting"].toInfoMessage())
                        val ui = FriendListUI(info, plugin, this)
                        Bukkit.getScheduler().callSyncMethod(plugin) {
                            ui.show(player)
                        }
                    }
                    close()
                }
            }
            28 -> {
                ServerCaller["showTutorialUI"]?.invoke(listOf(player))
            }
            29 -> {
                player.tip(getter["ui.others.armor.click"])
                close()
            }
        }
    }

}