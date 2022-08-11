package com.zhufu.opencraft

import com.zhufu.opencraft.TradeManager.printTradeError
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toComponent
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toTipMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.event.HoverEvent.ShowItem
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*
import javax.security.auth.Destroyable

class TradeInfo : Cloneable, Destroyable {
    constructor()
    constructor(id: Int) {
        this.id = id
    }

    constructor(seller: String) {
        this.seller = seller
    }

    constructor(seller: String, buyer: String) {
        this.seller = seller
        this.buyer = buyer
    }

    private var isDestroy = false
    var items: SellingItemInfo? = null
    private var validateInventory: TradeValidateInventory? = null
    private var seller: String? = null
    private var faceLocation: Location? = null
    fun getSeller() = seller
    fun setSeller(value: String?, ignoreInventoryItem: Boolean) {
        seller = value
        if (value != null && value != "server") {
            val it = Bukkit.getOfflinePlayer(UUID.fromString(value))
            if (faceLocation == null && it.isOnline)
                faceLocation = it.player!!.location

            validateInventory = TradeValidateInventory(this, faceLocation)
            if (items == null)
                throw IllegalAccessError("Items must not be null!")
            if (!ignoreInventoryItem) {
                if (it.isOnline)
                    it.player!!.setInventory(items!!.item, -items!!.amount)
                else
                    throw IllegalStateException("Player ${it.name} must be online because [ignoreInventoryItem] is set as [false].")
            }
        }
    }

    var buyer: String? = null
        private set

    fun setBuyer(buyer: String?, howMany: Int = items!!.amount, cost: Boolean = false): Boolean {
        this.buyer = buyer
        if (buyer == null)
            return false
        if (items == null)
            throw IllegalAccessError("Items must not be null!")
        if (seller == null)
            throw IllegalAccessError("Seller must not be null!")

        val it = Bukkit.getPlayer(UUID.fromString(buyer))
        val info = PlayerManager.findInfoByPlayer(it!!)
        if (info == null) {
            it.printTradeError("我们无法确定您是谁", id)
            return false
        }
        val playerCurrency = info.currency
        val prise = this.items!!.unitPrise * howMany
        if (playerCurrency < prise) {
            it.printTradeError("您的货币数量不足", id, false)
            return false
        }
        val what = items!!.item
        if (!it.setInventory(what, howMany)) {
            it.printTradeError("您没有足够的物品", id, false)
            return false
        }
        if (howMany != this.items!!.amount) {
            this.items!!.amount -= howMany
        }
        if (!cost)
            info.currency -= prise
        if (seller != "server") {
            val seller = PlayerManager.findOfflineInfoByPlayer(UUID.fromString(seller!!)) ?: return false
            seller.currency += prise
            if (seller.isOnline) {
                seller.onlinePlayerInfo!!.player.info("${it.name}购买了您的${items!!.item.type}×$howMany，从而使您获得了${prise}个货币")
            }
        }
        val sellerName: String = if (seller == "server") seller!!
        else Bukkit.getOfflinePlayer(UUID.fromString(seller)).name ?: "unknown"
        it.sendMessage(
            Component.text("您向")
                .append(sellerName.toTipMessage())
                .append("以".toComponent())
                .append(prise.toString().toInfoMessage())
                .append("个货币购买了".toComponent())
                .append(
                    what.displayName().style(TextUtil.TIP_STYLE)
                        .append("×$howMany".toTipMessage()
                            .hoverEvent { HoverEvent.showItem(what.type.key, what.amount) as HoverEvent<Any> })
                )
        )
        return true
    }

    fun cancel() {
        if (seller == null || seller == "server" || items == null)
            return
        val player = Bukkit.getPlayer(UUID.fromString(seller)) ?: return
        player.inventory.addItem(items!!.item.clone().also { it.amount = items!!.amount })
        destroy()
    }

    override fun destroy() {
        if (seller == null || seller == "server" || items == null)
            return
        validateInventory?.cancel(null)
        this.isDestroy = true
    }

    override fun isDestroyed(): Boolean = isDestroy

    var id: Int = -1

    var location: Location? = null

    override fun equals(other: Any?): Boolean {
        return other is TradeInfo
                && other.items == this.items
                && other.seller == this.seller
                && other.id == this.id
    }

    override fun hashCode(): Int {
        var result = items?.hashCode() ?: 0
        result = 31 * result + (seller?.hashCode() ?: 0)
        result = 31 * result + (buyer?.hashCode() ?: 0)
        result = 31 * result + id
        return result
    }

    companion object {
        const val NULL = "NULL"
        fun fromJson(reader: JsonReader): TradeInfo {
            reader.beginObject()
            val r = TradeInfo()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "uuid" -> r.id = reader.nextInt()
                    "buyer" -> r.buyer = reader.nextString()
                    "seller" -> r.seller = reader.nextString()
                    "item" -> r.items = SellingItemInfo.fromJson(reader)
                    "location" -> {
                        var x = 0
                        var y = 0
                        var z = 0
                        var world = Base.tradeWorld
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "x" -> x = reader.nextInt()
                                "y" -> y = reader.nextInt()
                                "z" -> z = reader.nextInt()
                                "world" -> {
                                    val text = reader.nextString()
                                    if (text != world.name)
                                        world = Bukkit.getWorld(text)!!
                                }
                            }
                        }
                        reader.endObject()
                        r.location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                    }

                    "face" -> {
                        var x = 0.0
                        var y = 0.0
                        var z = 0.0
                        var yaw = 0F
                        var pitch = 0F
                        var world = Base.tradeWorld
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "x" -> x = reader.nextDouble()
                                "y" -> y = reader.nextDouble()
                                "z" -> z = reader.nextDouble()
                                "yaw" -> yaw = reader.nextDouble().toFloat()
                                "pitch" -> pitch = reader.nextDouble().toFloat()
                                "world" -> {
                                    val text = reader.nextString()
                                    if (text != world.name)
                                        world = Bukkit.getWorld(text)!!
                                }
                            }
                        }
                        reader.endObject()
                        r.faceLocation = Location(world, x, y, z, yaw, pitch)
                    }
                }
            }
            reader.endObject()
            r.setSeller(r.seller, true)
            if (r.buyer == NULL)
                r.buyer = null
            if (r.seller == NULL)
                r.seller = null
            return r
        }
    }

    fun appendToJson(jsonWriter: JsonWriter) {
        jsonWriter
            .beginObject()
            .name("uuid").value(id)
            .name("buyer").value(buyer ?: NULL)
            .name("seller").value(seller ?: NULL)
        jsonWriter.name("item")
        items!!.appendToJson(jsonWriter)
        /**
         * Handle [location]
         */
        if (location != null) {
            jsonWriter
                .name("location")
                .beginObject()
                .name("x").value(location!!.x)
                .name("y").value(location!!.y)
                .name("z").value(location!!.z)
                .name("world").value(location!!.world!!.name)
                .endObject()
        }
        if (faceLocation != null) {
            jsonWriter
                .name("face")
                .beginObject()
                .name("x").value(faceLocation!!.x)
                .name("y").value(faceLocation!!.y)
                .name("z").value(faceLocation!!.z)
                .name("world").value(faceLocation!!.world!!.name)
                .name("yaw").value(faceLocation!!.yaw)
                .name("pitch").value(faceLocation!!.pitch)
                .endObject()

        }
        jsonWriter.endObject()
    }
}