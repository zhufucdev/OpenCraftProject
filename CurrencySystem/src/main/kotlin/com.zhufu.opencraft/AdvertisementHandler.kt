package com.zhufu.opencraft

import com.zhufu.opencraft.events.UserLoginEvent
import com.zhufu.opencraft.events.UserLogoutEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapView
import org.bukkit.plugin.Plugin
import java.io.File
import java.nio.file.Paths

object AdvertisementHandler : Listener {
    private val displays = hashMapOf<Player, AdDisplay>()
    private lateinit var plugin: Plugin

    fun init(plugin: Plugin) {
        this.plugin = plugin
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onPlayerLogin(event: UserLoginEvent) {
        handlePlayerAd(event.player)
    }

    @EventHandler
    fun onPlayerLogout(event: UserLogoutEvent) {
        handlePlayerDismiss(event.info.player)
    }

    private val ads: Map<File, Int>
        get() = Paths.get("plugins", "ads")
            .toFile()
            .let {
                if (!it.exists()) {
                    it.mkdir()
                    null
                } else {
                    it
                }
            }
            ?.listFiles()
            ?.mapNotNull {
                if (it.isHidden)
                    null
                else
                    it to (it.nameWithoutExtension.toIntOrNull() ?: return@mapNotNull null)
            }
            ?.toMap()
            ?: mapOf()

    private fun handlePlayerAd(player: Player) {
        if (displays.containsKey(player)) return
        val choice = kotlin.run {
            val ads = ads.entries
            if (ads.isEmpty()) {
                return
            }
            val weightSum = ads.sumOf { it.value }
            var min = ads.first()
            var choice: File? = null
            for (ad in ads) {
                if (Base.trueByPercentages(ad.value.toFloat() / weightSum)) {
                    choice = ad.key
                    break
                } else if (ad.value < min.value) {
                    min = ad
                }
            }
            return@run choice ?: min.key
        }
        val mapView = QRUtil.getImageMap(choice)
        // record original status
        val display = if (player.inventory.itemInOffHand.isEmpty) {
            AdDisplay(null, true)
        } else if (player.inventory.itemInMainHand.isEmpty) {
            AdDisplay(null, false)
        } else {
            AdDisplay(player.inventory.itemInOffHand, true)
        }
        displays[player] = display
        display.apply(player, mapView)

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            handlePlayerDismiss(player)
        }, 200)
    }

    private fun handlePlayerDismiss(player: Player) {
        displays.remove(player)?.dismiss(player)
    }
}

class AdDisplay(private val originalItem: ItemStack?, private val isOffhand: Boolean) {
    fun apply(player: Player, mapView: MapView) {
        val map = ItemStack(Material.FILLED_MAP).updateItemMeta<MapMeta> {
            setMapView(mapView)
        }

        if (isOffhand) {
            player.inventory.setItemInOffHand(map)
        } else {
            player.inventory.setItemInOffHand(map)
        }
    }

    fun dismiss(player: Player) {
        if (isOffhand) {
            player.inventory.setItemInOffHand(originalItem)
        } else {
            player.inventory.setItemInMainHand(originalItem)
        }
    }
}