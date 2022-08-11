package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.Game.env
import com.zhufu.opencraft.api.ServerCaller
import com.zhufu.opencraft.util.*
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
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
        Bukkit.createInventory(null, 36, (getter["ui.puTitle"] + "[${++id}]").toInfoMessage())

    init {
        inventory.apply {
            setItem(
                0,
                ItemStack(Material.PAPER).apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.statics.title"].toComponent().color(NamedTextColor.DARK_AQUA))
                    }
                }
            )
            setItem(
                1,
                ItemStack(Material.PLAYER_HEAD).apply {
                    itemMeta = (itemMeta as SkullMeta).apply {
                        owningPlayer = Bukkit.getOfflinePlayer(player.uniqueId)
                        displayName(
                            getter["ui.statics.face.title", player.name].toComponent().color(NamedTextColor.GREEN)
                        )
                        val chart = info?.let { Game.dailyChart.indexOf(it) } ?: -1
                        lore(
                            listOf(
                                getter["ui.statics.face.tip"].toTipMessage(),
                                (if (chart != -1)
                                    getter["ui.statics.face.chart", chart + 1]
                                else
                                    getter["ui.statics.face.noChart"]).toInfoMessage()
                            )
                        )
                    }
                }
            )
            setItem(
                3,
                ItemStack(Material.CLOCK).apply {
                    itemMeta = itemMeta!!.apply {
                        val rename = TextUtil.formatLore(TextUtil.format(info?.gameTime ?: 0, getter))
                        displayName(rename.first().toInfoMessage())
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
                        displayName(rename.first().toInfoMessage())
                        lore(rename.drop(1).map { it.toInfoMessage() })
                    }
                }
            )
            setItem(
                8,
                ItemStack(Material.BARRIER).apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.close"].toComponent().color(NamedTextColor.RED))
                    }
                }
            )

            setItem(
                9,
                ItemStack(Material.PAPER).apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.miniGame.title"].toComponent().color(NamedTextColor.DARK_AQUA))
                    }
                }
            )
            setItem(
                10,
                ItemStack(Material.IRON_PICKAXE).apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.miniGame.cw.title"].toComponent().color(NamedTextColor.RED))
                        val rename = TextUtil.formatLore(getter["ui.miniGame.cw.subtitle"])
                        addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        lore(rename.map { it.toComponent().color(NamedTextColor.DARK_AQUA) })
                    }
                }
            )
            setItem(
                11,
                ItemStack(Material.DIAMOND_SWORD).apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.miniGame.tms.title"].toInfoMessage().color(NamedTextColor.RED))
                        val rename = TextUtil.formatLore(getter["ui.miniGame.tms.subtitle"])
                        addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        lore(rename.map { it.toComponent().color(NamedTextColor.DARK_AQUA) })
                    }
                }
            )
            setItem(
                12,
                ItemStack(Material.PAPER).apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.miniGame.paper"].toSuccessMessage())
                    }
                }
            )
            setItem(
                18,
                ItemStack(Material.PAPER).apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.survival.title"].toInfoMessage().color(NamedTextColor.DARK_AQUA))
                    }
                }
            )
            setItem(
                19,
                ItemStack(Material.CHEST).apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.survival.grass.title"].toInfoMessage().color(NamedTextColor.GREEN))
                        var count = 0
                        val uuid = player.uniqueId.toString()
                        BlockLockManager.forEach {
                            if (it.owner == uuid)
                                count++
                        }
                        lore(
                            listOf(
                                getter["ui.survival.grass.subtitle", count].toInfoMessage(),
                                getter["ui.survival.grass.click"].toTipMessage()
                            )
                        )
                    }
                }
            )
            setItem(
                20,
                ItemStack(Material.ENDER_PEARL).apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.survival.pearl.title"].toInfoMessage().color(NamedTextColor.AQUA))
                        lore(
                            listOf(
                                getter["ui.survival.pearl.subtitle"].toInfoMessage(),
                                getter["ui.survival.pearl.click"].toTipMessage()
                            )
                        )
                    }
                }
            )
            setItem(
                21,
                ItemStack(Material.GRASS_BLOCK).updateItemMeta<ItemMeta> {
                    displayName(getter["ui.world.title"].toSuccessMessage())
                    lore(listOf(getter["ui.world.click"].toTipMessage()))
                }
            )
            setItem(
                22,
                ItemStack(Material.PLAYER_HEAD).updateItemMeta<ItemMeta> {
                    displayName(
                        getter["ui.friend.title"].toComponent().color(NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD)
                    )
                    lore(listOf(getter["ui.friend.check"].toTipMessage()))
                }
            )
            setItem(
                27,
                ItemStack(Material.PAPER).apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.others.title"].toComponent().color(NamedTextColor.DARK_AQUA))
                    }
                }
            )
            setItem(
                28,
                ItemStack(Material.SNOWBALL).apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.others.snowball.title"].toComponent().color(NamedTextColor.DARK_AQUA))
                        lore(listOf(
                            getter["ui.others.snowball.subtitle"].toInfoMessage(),
                            getter["ui.others.snowball.click"].toTipMessage()
                        ))
                    }
                }
            )
            setItem(
                29,
                ItemStack(Material.ARMOR_STAND).apply {
                    itemMeta = itemMeta!!.apply {
                        displayName(getter["ui.others.armor.title"].toComponent().color(NamedTextColor.DARK_AQUA))
                        lore(listOf(
                            getter["ui.others.armor.subtitle", info?.skin ?: player.name].toInfoMessage(),
                            getter["ui.others.armor.click"].toTipMessage()
                        ))
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
                        player.sendActionBar(getter["ui.chart.booting"].toInfoMessage())
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
                        player.sendActionBar(getter["ui.friend.booting"].toInfoMessage())
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