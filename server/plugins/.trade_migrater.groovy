import bukkit.Server
import com.google.gson.JsonParser
import com.zhufu.opencraft.Base
import com.zhufu.opencraft.TradeManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration

static void importTradeInfo() {
    def tradeConfig = new File("./plugins/trade/tradeInfos.json")
    def configReader = new FileReader(tradeConfig)

    def trades = JsonParser.parseReader(configReader).asJsonArray
    def server = Base.INSTANCE.serverID
    def NULL = "NULL"

    trades.forEach {
        def current = it.asJsonObject
        def seller = current.get("seller").asString.with { s -> if (s == "server") server else UUID.fromString(s) }
        def buyer = current.get("buyer").asString.with { b -> if (b == NULL) null else UUID.fromString(b) }
        def sellingInfo = current.get("item").asJsonObject
        def item =
                YamlConfiguration
                        .loadConfiguration(new StringReader(sellingInfo.get("item").asString))
                        .getItemStack("YAML")
        def amount = sellingInfo.get("amount").asInt
        def prise = sellingInfo.get("prise").asLong
        def location = current.getAsJsonObject("location")?.with {
            new Location(
                    Bukkit.getWorld(it.get("world").asString),
                    it.get("x").asDouble,
                    it.get("y").asDouble,
                    it.get("z").asDouble
            ).tap {
                def facing = current.getAsJsonObject("face")
                it.yaw = facing.get("yaw").asFloat
                it.pitch = facing.get("pitch").asFloat
            }
        }
        def result = TradeManager.INSTANCE.trade(
                seller,
                item,
                amount,
                prise,
                buyer,
                location,
                true
        )
        if (buyer != null)
            result.destroy()
    }
}

Server.runSync {
    importTradeInfo()
}