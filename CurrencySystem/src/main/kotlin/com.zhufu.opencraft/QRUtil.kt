package com.zhufu.opencraft

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.map.MapView
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.concurrent.Executors
import kotlin.collections.HashMap
import kotlin.math.roundToLong

object QRUtil : Listener {
    private val threadPool = Executors.newCachedThreadPool()
    fun init(plugin: Plugin){
        plugin.server.pluginManager.registerEvents(this,plugin)
    }
    private val qrMap = HashMap<File,MapView>()

    class PlayerDonationInfo(val player: Player,val originItem: ItemStack,val time: Long){
        fun remove(){
            player.inventory.setItemInMainHand(originItem)
        }

        fun treat(amount: Float){
            val info = PlayerManager.findInfoByPlayer(player)
            if (info == null){
                player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
                return
            }
            val add = (info.gameTime/1200*3*amount).roundToLong()
            info.currency += add
            player.sendMessage(TextUtil.success("感谢您对我们的支持，已给予您${add}货币作为奖励"))
            remove()
        }

        override fun equals(other: Any?): Boolean {
            return other is PlayerDonationInfo && other.player == this.player && other.time == this.time
        }

        override fun hashCode(): Int {
            var result = player.hashCode()
            result = 31 * result + originItem.hashCode()
            result = 31 * result + time.hashCode()
            return result
        }
    }
    var qrViewer: PlayerDonationInfo? = null

    @EventHandler
    fun onPlayerClick(event: PlayerInteractEvent){
        if (qrViewer?.player != event.player)
            return
        if (event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.RIGHT_CLICK_AIR){
            qrViewer?.remove()
            qrViewer = null
        }
    }

    fun getImageMap(image: File): MapView {
        if (qrMap.containsKey(image)){
            return qrMap[image]!!
        }
        val result = Bukkit.createMap(Base.spawnWorld)
        result.renderers.forEach {
            result.removeRenderer(it)
        }
        result.addRenderer(ImageRender(image))
        qrMap[image] = result
        return result
    }

    fun sendToPlayer(image: File,player: Player){
        Bukkit.getScheduler().runTaskAsynchronously(CurrencySystem.instance, Runnable{
            if (!CurrencySystem.isServerReady){
                player.sendMessage(TextUtil.error("抱歉，但您不能在此时进行捐赠，因为主机与服务器还没有建立连接"))
                player.sendMessage(TextUtil.tip("如有需要，请联系服务器管理员"))
                return@Runnable
            }

            if (qrViewer != null){
                qrViewer!!.remove()
                qrViewer!!.player.sendMessage(TextUtil.error("抱歉，但您的操作已超时，这取决于其他玩家是否在进行捐赠"))
            }
            val mapView = getImageMap(image)
            val map = ItemStack(Material.MAP)
            val meta = map.itemMeta
            (meta as Damageable).damage = mapView.id
            map.itemMeta = meta

            qrViewer = PlayerDonationInfo(player,player.inventory.itemInMainHand,System.currentTimeMillis())
            player.inventory.setItemInMainHand(map)
            player.sendActionText(TextUtil.success("右键以关闭"))
        })
    }
}