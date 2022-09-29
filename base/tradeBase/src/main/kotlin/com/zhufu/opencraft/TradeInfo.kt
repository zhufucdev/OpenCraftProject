package com.zhufu.opencraft

import com.zhufu.opencraft.data.Database
import com.zhufu.opencraft.inventory.NPCExistence
import com.zhufu.opencraft.player_community.MessagePool
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toComponent
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toTipMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import org.bson.Document
import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.Instant
import java.util.*
import javax.security.auth.Destroyable

class TradeInfo : Cloneable, Destroyable {
    val id: UUID
    val item: SellingItemInfo
    var seller: UUID? = null
        private set
    var buyer: UUID? = null
        private set
    var location: Location? = null
    val startTime: Instant
    var endTime: Instant? = null
        private set

    constructor(items: SellingItemInfo) {
        this.id = UUID.randomUUID()
        this.item = items
        this.startTime = Instant.now()
    }

    private var isDestroyed = false
    private var validateInventory: NPCExistence? = null
    private var faceLocation: Location? = null

    private constructor(doc: Document) {
        this.id = doc.get("_id", UUID::class.java)
        this.item = SellingItemInfo.of(doc.get("item", Document::class.java))
        this.seller = doc.get("seller", UUID::class.java)
        this.buyer = doc.get("buyer", UUID::class.java)
        this.faceLocation = doc.get("face", Document::class.java)?.let { Location.deserialize(it) }
        this.location = doc.get("location", Document::class.java)?.let { Location.deserialize(it) }
        this.isDestroyed = doc.getBoolean("destroyed", false)
        this.startTime = Instant.ofEpochSecond(doc.getLong("start"))
        this.endTime = doc.getLong("end")?.let { Instant.ofEpochSecond(it) }
    }

    fun setSeller(value: UUID?, ignoreInventoryItem: Boolean = false, ignoreNPC: Boolean = false) {
        seller = value
        Database.trade(id, toDocument())
        if (value != null && value != Base.serverID) {
            val player = Bukkit.getOfflinePlayer(value)
            if (faceLocation == null && player.isOnline)
                faceLocation = player.player!!.location

            if (!ignoreNPC)
                summonNPC()
            if (!ignoreInventoryItem) {
                if (player.isOnline)
                    player.player!!.setInventory(item.item, -item.amount)
                else
                    throw IllegalStateException("Player ${player.name} must be online because [ignoreInventoryItem] is set as [false].")
            }
        }
    }

    fun summonNPC() {
        validateInventory = NPCExistence.impl(this, faceLocation)
    }

    fun setBuyer(buyer: UUID?, howMany: Int = item.amount, cost: Boolean = false): Boolean {
        this.buyer = buyer
        this.endTime = Instant.now()
        Database.trade(id, toDocument())

        if (buyer == null)
            return false
        val seller = seller ?: throw IllegalAccessError("Seller must be set.")

        val player = Bukkit.getPlayer(buyer)
        val info = player!!.info()
        val getter = info.getter()
        if (info == null) {
            player.error(getter["player.error.unknown"])
            return false
        }
        val playerCurrency = info.currency
        val prise = this.item.unitPrise * howMany
        if (playerCurrency < prise) {
            player.error(getter["trade.error.poor"])
            return false
        }
        val what = item.item
        if (!player.setInventory(what, howMany)) {
            player.error(getter["trade.error.lackOfItem"])
            return false
        }
        if (howMany != this.item.amount) {
            this.item.amount -= howMany
        }
        if (!cost)
            info.currency -= prise
        if (seller != Base.serverID) {
            val sellerInfo = PlayerManager.findOfflineInfoByPlayer(seller) ?: return false
            sellerInfo.currency += prise
            sellerInfo.messagePool.add(
                "${player.name}购买了您的${item.item.type}×$howMany，从而使您获得了${prise}个货币",
                MessagePool.Type.System
            )
        }
        val sellerName: String = if (seller == Base.serverID) "服务器"
        else Bukkit.getOfflinePlayer(seller).name ?: "未知"
        player.sendMessage(
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

    /**
     * Destroy and remove trade record.
     */
    fun cancel() {
        destroy()
        TradeManager.remove(this)
    }

    fun toDocument(): Document = Document()
        .append("_id", id)
        .append("item", item.toDocument())
        .append("seller", seller)
        .append("buyer", buyer)
        .append("face", faceLocation?.serialize()?.let { Document(it) })
        .append("location", location?.serialize()?.let { Document(it) })
        .append("destroyed", isDestroyed)
        .append("start", startTime.epochSecond)
        .append("end", endTime?.epochSecond)

    /**
     * Finish trade, deleting NPC.
     */
    override fun destroy() {
        this.isDestroyed = true
        validateInventory?.destroy()
        Database.trade(id, toDocument())
    }

    override fun isDestroyed(): Boolean = isDestroyed

    override fun equals(other: Any?): Boolean {
        return other is TradeInfo
                && other.item == this.item
                && other.seller == this.seller
                && other.id == this.id
    }

    override fun hashCode(): Int {
        var result = item.hashCode()
        result = 31 * result + (seller?.hashCode() ?: 0)
        result = 31 * result + (buyer?.hashCode() ?: 0)
        result = 31 * result + id.hashCode()
        return result
    }

    companion object {
        fun of(doc: Document) = TradeInfo(doc)
    }
}