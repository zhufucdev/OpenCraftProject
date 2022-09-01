package com.zhufu.opencraft.ui

import com.zhufu.opencraft.*
import com.zhufu.opencraft.api.Nameable
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.inventory.PayInputDialog
import com.zhufu.opencraft.inventory.PaymentDialog
import com.zhufu.opencraft.util.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class AdvertisementEditUI(
    plugin: Plugin, private val owner: Info, private val ad: Advertisement? = null,
    override val parentInventory: IntractableInventory? = null
) : IntractableInventory(plugin), Backable {
    private val editMode = ad != null
    private val backable = parentInventory != null

    val getter = owner.getter()
    override val inventory: Inventory = Bukkit.createInventory(null, 27, getter["ad.title"].toInfoMessage())
    private var config = (ad?.clone() as Advertisement?) ?: Advertisement(
        owner = owner,
        duration = Advertisement.Duration.TEN_SEC,
        size = Advertisement.Size.ADAPTIVE
    )
    private var imageUrl: AdUrl = AdUrl("")
    private val prise: Long get() = config.unitPrise - (ad?.unitPrise ?: 0)
    private val isReady get() = editMode || imageUrl.name.isNotEmpty()

    init {
        builder {
            // Size
            set(0, 0, ItemStack(Material.FILLED_MAP).updateItemMeta<ItemMeta> {
                displayName(getter["ad.size.title"].toInfoMessage())
            })
            updateSize()
            // Duration
            set(0, 1, ItemStack(Material.CLOCK).updateItemMeta<ItemMeta> {
                displayName(getter["ad.duration.title"].toInfoMessage())
            })
            updateDuration()
            // Bonus
            updateBonus()
            // Misc
            set(0, 2, ItemStack(Material.SPYGLASS).updateItemMeta<ItemMeta> {
                displayName(getter["ad.url.title"].toInfoMessage())
                lore(TextUtil.formatLore(getter["ad.url.des"]).map { it.toTipMessage() })
            })
            set(2, 2, ItemStack(Material.PAPER).updateItemMeta<ItemMeta> {
                displayName(getter["ad.tip.title"].toInfoMessage())
                lore(getter["ad.tip.body"].split('\n').map { it.toTipMessage() })
            })
            updateSummary()
            updateName()

            val back = Widgets.back.updateItemMeta<ItemMeta> {
                displayName(getter["ui.back"].toInfoMessage())
            }
            if (editMode) {
                val cancel = Widgets.cancel.updateItemMeta<ItemMeta> {
                    displayName(getter["ad.cancel.title"].toErrorMessage())
                    lore(listOf(getter["ad.cancel.des"].toTipMessage()))
                }
                set(6, 2, cancel)
                if (backable)
                    set(5, 2, back)
            } else {
                if (backable)
                    set(6, 2, back)
            }
        }
    }

    override fun onClick(event: InventoryClickEvent) {
        if (event.clickedInventory != inventory) {
            return
        }
        when (event.rawSlot) {
            in 3..5 -> {
                // size
                val index = event.rawSlot - 3
                val size = Advertisement.Size.values()[index]
                config.size = size
                updateSize()
            }

            in 12..14 -> {
                // duration
                val index = event.rawSlot - 12
                val duration = Advertisement.Duration.values()[index]
                config.duration = duration
                updateDuration()
            }

            18 -> handleImage()

            19 -> {
                // bonus
                PayInputDialog(plugin, owner, ItemStack(Material.GOLD_INGOT).updateItemMeta<ItemMeta> {
                    displayName(getter["ad.bonus.title"].toInfoMessage())
                    lore(
                        TextUtil
                            .formatLore(getter["ad.bonus.subtitle"])
                            .map { it.toComponent() }
                    )
                })
                    .setOnConfirmListener {
                        config.bonus = amount.toLong()
                        updateBonus()
                        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                            this@AdvertisementEditUI.show()
                        }, 3)
                        false
                    }
                    .show()
            }

            26 -> {
                if (!isReady) {
                    return
                }
                PaymentDialog(
                    showingTo!!,
                    SellingItemInfo(
                        ItemStack(Material.ITEM_FRAME).updateItemMeta<ItemMeta> {
                            displayName(getter["ad.title"].toTitleMessage())
                        },
                        prise,
                        1
                    ),
                    TradeManager.getNewID(),
                    plugin,
                    this
                )
                    .setOnPayListener { success ->
                        if (success) {
                            config.apply {
                                if (!editMode)
                                    time().update()
                                else {
                                    enabled = true // to change debated state
                                    update()
                                }
                                // this can be reached when barely renaming the ad
                                if (editMode && imageUrl.name.isNotEmpty())
                                    Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                                        postUpload(cacheImage(imageUrl.name))
                                    })
                            }
                        } else {
                            owner.player.error(getter["trade.error.poor"])
                        }
                        true
                    }
                    .show()
            }

            else -> {
                if (editMode) {
                    when (event.rawSlot) {
                        23 -> if (backable) back(showingTo!!)
                        24 -> {
                            config.cancel()
                            close()
                            owner.player.info(getter["ad.deleted"])
                        }
                        25 -> handleRename()
                    }
                } else {
                    if (event.rawSlot == 25) {
                        handleRename()
                    } else if (event.rawSlot == 24 && backable) {
                        back(showingTo!!)
                    }
                }
            }
        }
    }

    private fun handleRename() {
        RenameHandler
            .select(owner.player, config)
            .setPostRenameListener {
                updateName()
                show()
            }
        owner.player.tip(getter["ad.rename"])
        close()
    }

    private fun handleImage() {
        RenameHandler
            .select(owner.player, imageUrl)
            .setPostRenameListener {
                updateSummary()
                show()
            }
        owner.player.info(getter["ad.url.tip"])
        close()
    }

    private fun postUpload(cacheResult: CacheResultContent) {
        when (cacheResult.first) {
            Advertisement.CacheResult.ERROR -> {
                owner.player.error(getter["ad.error.imageFailed", cacheResult.second!!.message])
            }

            Advertisement.CacheResult.DENIED -> {
                owner.player.error(getter["ad.error.protocol"])
            }

            Advertisement.CacheResult.SUCCESS -> {
                owner.player.success(getter["ad.success"])
            }
        }
    }

    private fun updateSummary() {
        val item = if (isReady) Widgets.confirm else ItemStack(Material.GRAY_DYE)
        inventory.setItem(26, item.updateItemMeta<ItemMeta> {
            displayName(getter["ui.confirm"].toSuccessMessage())
            val sum = getter["ad.summary.${if (editMode) "once" else "weekly"}", prise].toInfoMessage()
            lore(
                if (isReady) listOf(sum)
                else listOf(sum, getter["ad.error.noImage"].toErrorMessage())
            )
        })
    }

    private fun updateSize() {
        Advertisement.Size.values().forEachIndexed { index, size ->
            val item =
                if (config.size == size)
                    Widgets.confirm.updateItemMeta<ItemMeta> {
                        displayName(getter["ad.size.${size.name.lowercase()}.name"].toSuccessMessage())
                        lore(
                            TextUtil.formatLore(getter["ad.size.${size.name.lowercase()}.des"])
                                .map { it.toTipMessage() }
                                .plus(getter["ad.selected"].toTipMessage())
                        )
                    }
                else
                    ItemStack(Material.GRAY_DYE).updateItemMeta<ItemMeta> {
                        displayName(getter["ad.size.${size.name.lowercase()}.name"].toInfoMessage())
                        lore(
                            TextUtil.formatLore(getter["ad.size.${size.name.lowercase()}.des"])
                                .map { it.toTipMessage() }
                        )
                    }
            inventory.setItem(3 + index, item)
        }
        updateSummary()
    }

    private fun updateDuration() {
        Advertisement.Duration.values().forEachIndexed { index, duration ->
            val item =
                if (config.duration == duration)
                    Widgets.confirm.updateItemMeta<ItemMeta> {
                        displayName(
                            getter["ad.duration.${duration.name.removeSuffix("_SEC").lowercase()}"]
                                .toSuccessMessage()
                        )
                        lore(listOf(getter["ad.selected"].toTipMessage()))
                    }
                else
                    ItemStack(Material.GRAY_DYE).updateItemMeta<ItemMeta> {
                        displayName(
                            getter["ad.duration.${duration.name.removeSuffix("_SEC").lowercase()}"]
                                .toInfoMessage()
                        )
                    }
            inventory.setItem(12 + index, item)
        }
        updateSummary()
    }

    private fun updateBonus() {
        inventory.setItem(19, ItemStack(Material.GOLD_INGOT).updateItemMeta<ItemMeta> {
            displayName(getter["ad.bonus.title"].toInfoMessage())
            lore(
                TextUtil.formatLore(getter["ad.bonus.subtitle"]).map { it.toComponent() }
                    .plus(getter["ad.bonus.tip", config.bonus].toTipMessage())
            )
        })
        updateSummary()
    }

    private fun updateName() {
        inventory.setItem(25, Widgets.rename.updateItemMeta<ItemMeta> {
            displayName(getter["ui.rename"].toInfoMessage())
            config.name.takeIf { it.isNotEmpty() }?.let {
                lore(listOf(getter["ad.name", it].toComponent()))
            }
        })
    }

    fun show() = show(owner.player)
}

class AdUrl(override var name: String) : Nameable