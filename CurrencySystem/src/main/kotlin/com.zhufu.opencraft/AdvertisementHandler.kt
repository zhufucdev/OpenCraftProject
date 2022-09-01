package com.zhufu.opencraft

import com.zhufu.opencraft.events.UserLoginEvent
import com.zhufu.opencraft.events.UserLogoutEvent
import com.zhufu.opencraft.util.toInfoMessage
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapView
import org.bukkit.plugin.Plugin

object AdvertisementHandler : Listener {
    private val displays = hashMapOf<Player, AdDisplay>()
    private lateinit var plugin: Plugin

    fun init(plugin: Plugin) {
        this.plugin = plugin
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun close() {
        displays.keys.forEach { handlePlayerDismiss(it) }
    }

    @EventHandler
    fun onPlayerLogin(event: UserLoginEvent) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            handlePlayerAd(event.player)
        }, 100)
    }

    @EventHandler
    fun onPlayerLogout(event: UserLogoutEvent) {
        handlePlayerDismiss(event.info.player)
    }

    @EventHandler
    fun onPlayerSwapAd(event: PlayerSwapHandItemsEvent) {
        if (displays.containsKey(event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerSwipeHotBar(event: PlayerItemHeldEvent) {
        if (displays.containsKey(event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerDropAd(event: PlayerDropItemEvent) {
        if (displays.containsKey(event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerClickAd(event: InventoryClickEvent) {
        val player = Bukkit.getPlayer(event.whoClicked.uniqueId)!!
        if (displays.containsKey(player)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDieWithAd(event: PlayerDeathEvent) {
        event.drops.removeIf { NBTItem(it).getBoolean("ad") == true }
    }

    private fun handlePlayerAd(player: Player) {
        if (displays.containsKey(player)) return
        val choice = kotlin.run {
            val ads = Advertisement.list()
            if (ads.firstOrNull() == null) {
                return
            }
            val weightSum = ads.sumOf { it.weight }
            var min = ads.first()
            var choice: Advertisement? = null
            for (ad in ads) {
                if (Base.trueByPercentages((ad.weight / weightSum).toFloat())) {
                    choice = ad
                    break
                } else if (ad.weight < min.weight) {
                    min = ad
                }
            }
            return@run choice ?: min
        }
        try {
            val mapView = QRUtil.getImageMap(choice.image)
            // record original status
            val display = when (choice.size) {
                Advertisement.Size.SMALL -> AdDisplay(player,true)
                Advertisement.Size.ADAPTIVE ->
                    if (player.inventory.let { it.itemInOffHand.isEmpty || it.itemInMainHand.isEmpty })
                        AdDisplay(player, false)
                    else
                        AdDisplay(player, true)
                Advertisement.Size.LARGE -> AdDisplay(player, false)
            }
            displays[player] = display
            display.apply(mapView)
            player.sendActionBar(player.getter()["ad.display", choice.owner.name].toInfoMessage())

            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                handlePlayerDismiss(player)
            }, 200)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to show ad image ${choice.name} to ${player.name}.")
            e.printStackTrace()
        }
    }

    private fun handlePlayerDismiss(player: Player) {
        displays.remove(player)?.dismiss()
    }
}

class AdDisplay(val player: Player, private val isOffhand: Boolean) {
    private val originalItems = player.inventory.let { it.itemInMainHand to it.itemInOffHand }
    fun apply(mapView: MapView) {
        val map = ItemStack(Material.FILLED_MAP).updateItemMeta<MapMeta> {
            setMapView(mapView)
        }
        val nbt = NBTItem(map, true)
        nbt.setBoolean("ad", true)

        if (isOffhand) {
            player.inventory.apply {
                setItemInOffHand(map)
                setItemInMainHand(null)
            }
        } else {
            player.inventory.apply {
                setItemInMainHand(map)
                setItemInOffHand(null)
            }
        }
    }

    fun dismiss() {
        player.inventory.setItemInMainHand(originalItems.first)
        player.inventory.setItemInOffHand(originalItems.second)
    }
}