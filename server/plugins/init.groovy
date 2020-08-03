import bukkit.Content
import bukkit.Server
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.event.server.ServerListPingEvent
import zhufucdev.gun.DamageableBlock

Server.listenEvent(ServerListPingEvent.class) {
    serverIcon = Bukkit.loadServerIcon(new File("logo.png"))
    motd = ChatColor.BLUE.toString() + "OpenCraft Test Server."
}

Content.defineBlock {
    name 'DamageableBlock'
    existsAs DamageableBlock.class
}

