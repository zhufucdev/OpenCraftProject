import bukkit.Server
import com.zhufu.opencraft.data.Database
import com.zhufu.opencraft.util.TextUtil
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.server.ServerListPingEvent
import ss.Logger

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import static com.mongodb.client.model.Filters.eq
import org.bson.Document

Server.listenEvent(ServerListPingEvent.class) {
    serverIcon = Bukkit.loadServerIcon(new File("logo.png"))
    motd = ChatColor.BLUE.toString() + "OpenCraft " + TextUtil.INSTANCE.getColoredText("Dev\n", TextUtil.TextColor.GOLD, false, true) + TextUtil.INSTANCE.info("For Developers")
}