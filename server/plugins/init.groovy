import bukkit.Server
import com.zhufu.opencraft.util.TextUtil
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.event.server.ServerListPingEvent

def map = new HashMap<InetAddress, Integer>()
Server.listenEvent(ServerListPingEvent.class) {
    serverIcon = Bukkit.loadServerIcon(new File("logo.png"))
    motd = ChatColor.BLUE.toString() + "OpenCraft " + TextUtil.INSTANCE.getColoredText("Dev\n", TextUtil.TextColor.GOLD, false, true) + TextUtil.INSTANCE.info("For Developers")
}