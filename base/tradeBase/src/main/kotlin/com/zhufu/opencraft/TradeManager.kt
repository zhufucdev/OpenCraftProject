package com.zhufu.opencraft

import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

object TradeManager {
    /**
     * Check if the total prise reaches the limit of int
     * @return true if reaches the limit
     */
    fun checkLimit(itemInfo: SellingItemInfo): Boolean {
        val long = itemInfo.unitPrise * itemInfo.amount.toLong()
        return long > Int.MAX_VALUE
    }
    fun checkLimit(unitPrise: Long,amount: Int): Boolean {
        val long = unitPrise * amount
        return long > Int.MAX_VALUE
    }

    @Deprecated("Outdated.")
    fun HumanEntity.printTradeError(msg: String, tradeID: Int, isSerous: Boolean = true) {
        sendMessage(TextUtil.error("ID为${tradeID}的交易失败: $msg"))
        if (isSerous) sendMessage(TextUtil.tip("我们对此表示抱歉，如果影响您的游戏进展，请联系服务器管理员"))
    }

    fun loadTradeCompass(player: Info){
        if (player.player.world == Base.tradeWorld) {
            player.inventory.create(DualInventory.RESET).load(inventoryOnly = true)
            player.player.inventory.addItem(
                    ItemStack(Material.COMPASS)
                            .also {
                                it.itemMeta = it.itemMeta!!.apply { setDisplayName(TextUtil.getColoredText(Language[player, "trade.explorer"], TextUtil.TextColor.RED, false, underlined = false)) }
                            }
            )
        }
    }

    var plugin: JavaPlugin? = null
    val isInit: Boolean
        get() = plugin == null

    fun init(plugin: JavaPlugin) {
        TradeManager.plugin = plugin
    }

    private val mList = ArrayList<TradeInfo>()

    val size: Int
        get() = mList.size
    fun forEach(l: (TradeInfo) -> Unit) = mList.forEach(l)
    fun firstOrNull(l: (TradeInfo) -> Boolean) = mList.firstOrNull(l)
    operator fun get(player: HumanEntity): List<TradeInfo> = mList.filter { it.getSeller() == player.uniqueId.toString() }

    fun sell(seller: String, what: ItemStack, amount: Int, unitPrise: Long, buyer: Player? = null, location: Location? = null, ignoreInventoryItem: Boolean = false) {
        val id = getNewID()
        if (seller != "server") {
            val sellerPlayer = Bukkit.getPlayer(UUID.fromString(seller))
            sellerPlayer!!.sendMessage(TextUtil.info("您正在以${unitPrise}出售${if (what.itemMeta?.hasLocalizedName() == true) what.itemMeta!!.localizedName else what.type.name}×$amount"))
            Bukkit.getOnlinePlayers().filter { it.uniqueId.toString() != seller }.forEach {
                it.sendMessage(TextUtil.info("${sellerPlayer.name}正在以${unitPrise}出售${what.type.name}×$amount"))
            }
        }
        add(
                TradeInfo(id)
                        .apply {
                            this.location = location
                            items = SellingItemInfo(item = what, unitPrise = unitPrise, amount = amount)
                            setSeller(seller, ignoreInventoryItem)
                            setBuyer(buyer?.uniqueId?.toString(), items!!.amount)
                        }
        )
    }

    private fun add(tradeInfo: TradeInfo){
        mList.add(tradeInfo)
    }

    enum class TradeResult {
        SUCCESSFUL,FAILED,UPDATE
    }
    fun buy(who: HumanEntity, id: Int,howMany: Int): TradeResult {
        val t = mList.firstOrNull { it.id == id }?:return TradeResult.FAILED
        val amount = t.items!!.amount

        val r = t.setBuyer(who.uniqueId.toString(),howMany = howMany)
        return if (!r)
            TradeResult.FAILED
        else {
            if (amount == howMany) {
                mList.remove(t)
                TradeResult.SUCCESSFUL
            } else TradeResult.UPDATE
        }
    }

    fun cancel(info: TradeInfo){
        info.cancel()
        mList.remove(info)
    }

    fun destroy(info: TradeInfo){
        info.destroy()
        mList.remove(info)
    }

    fun saveToFile(file: File) {
        if (!file.parentFile.exists())
            file.parentFile.mkdirs()
        if (!file.exists())
            file.createNewFile()
        val gson = GsonBuilder().setPrettyPrinting()
                .create()
        val writer = gson.newJsonWriter(file.writer())
        writer.beginArray()
        mList.forEach {
            it.appendToJson(writer)
        }
        writer.endArray()
        writer.flush()
    }

    fun loadFromFile(file: File) {
        if (!file.exists()) {
            file.createNewFile()
            return
        }
        val reader = JsonReader(file.reader())
        reader.beginArray()
        while (reader.hasNext()) {
            try {
                mList.add(TradeInfo.fromJson(reader))
            }catch (e: IllegalArgumentException){
                Bukkit.getLogger().warning("Could not load ${reader.path}.")
                e.printStackTrace()
            }
        }
        reader.endArray()
        reader.close()

        presentID = (mList.maxBy { it.id }?.id?:-1)+1
    }

    var presentID = 0
    fun getNewID(): Int = ++presentID
}