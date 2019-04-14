package com.zhufu.opencraft

import com.zhufu.opencraft.TradeManager.printTradeError
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import javax.security.auth.Destroyable

class TradeInfo: Cloneable,Destroyable {
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
    var validateInventory: TradeValidateInventory? = null
    private var seller: String? = null
    fun getSeller() = seller
    fun setSeller(value: String?, ignoreInventoryItem: Boolean) {
        seller = value
        if (value != null && value != "server") {
            validateInventory = TradeValidateInventory(this)
            val it = Bukkit.getPlayer(UUID.fromString(value))?:return
            if (items == null)
                throw IllegalAccessError("Items must not be null!")
            if (!ignoreInventoryItem)
                it.setInventory(items!!.item, -items!!.amount)
        }
    }

    private var buyer: String? = null
    fun getBuyer() = buyer
    fun setBuyer(buyer: String?, howMany: Int = items!!.amount): Boolean {
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
        if (howMany != this.items!!.amount){
            this.items!!.amount -= howMany
        }

        info.currency -= prise
        if (seller != "server") {
            val seller = PlayerManager.findOfflinePlayer(UUID.fromString(seller!!))?:return false
            seller.currency += prise
            if (seller.isOnline){
                seller.onlinePlayerInfo!!.player.sendMessage(TextUtil.info("${it.name}购买了您的${items!!.item.type}×$howMany，从而使您获得了${prise}个货币"))
            }
        }
        val sellerName: String = if (seller == "server") seller!!
                        else Bukkit.getOfflinePlayer(UUID.fromString(seller)).name?:"unknown"
        it.sendMessage("您向${TextUtil.tip(sellerName)}以${TextUtil.tip(prise.toString())}个货币购买了${TextUtil.tip("${what.type}×$howMany")}")
        return true
    }

    fun cancel(){
        if (seller == null || seller == "server" || items == null)
            return
        val player = Bukkit.getPlayer(UUID.fromString(seller))?:return
        player.inventory.addItem(items!!.item.clone().also { it.amount = items!!.amount })
        destroy()
    }

    override fun destroy(){
        if (seller == null || seller == "server" || items == null)
            return
        validateInventory?.cancel(null)
        this.isDestroy = true
    }

    override fun isDestroyed(): Boolean = isDestroy

    var id: Int = -1

    var location: Location? = null

    private fun Player.setInventory(type: ItemStack, amount: Int): Boolean {
        if (amount < 0) {
            val sub = -amount
            var aAmount = 0
            this.inventory.forEach {
                if (it != null && it.type == type.type)
                    aAmount += it.amount
            }
            if (aAmount < sub) {
                return false
            }
            var removed = 0
            for (i in 0 until this.inventory.size) {
                val itemStack = this.inventory.getItem(i)
                if (itemStack != null && itemStack.type == type.type) {
                    if (itemStack.amount >= sub - removed) {
                        itemStack.amount -= sub - removed
                        removed += sub - removed
                    } else {
                        removed += itemStack.amount
                        this.inventory.setItem(i, null)
                    }
                    if (removed >= sub) {
                        (removed - sub).also { add ->
                            if (add > 0)
                                this.inventory.addItem(ItemStack(type.type, add))
                        }
                        break
                    }
                }
            }
        } else {
            this.inventory.addItem(
                    type.clone()
                            .also { it.amount = amount }
            )
        }
        return true
    }

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
                                    if (text != "world_trade")
                                        world = Bukkit.getWorld(text)!!
                                }
                            }
                        }
                        reader.endObject()
                        r.location = Location(world,x.toDouble(),y.toDouble(),z.toDouble())
                    }
                }
            }
            reader.endObject()
            r.setSeller(r.seller,true)
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
        jsonWriter.endObject()
    }
}