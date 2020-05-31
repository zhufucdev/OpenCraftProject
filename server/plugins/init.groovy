import bukkit.Server
import org.bukkit.ChatColor
import org.bukkit.event.server.ServerListPingEvent

Server.listenEvent(ServerListPingEvent.class) {
    motd = ChatColor.BLUE.toString() + "OpenCraft Test Server."
}