import bukkit.Server
import com.zhufu.opencraft.util.TextUtil
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.event.server.ServerListPingEvent

import java.text.SimpleDateFormat

def map = new HashMap<InetAddress, Integer>()
def speeches = ["我们使用了新的硬件", "并将服务器合并进入新的域名", "新的游戏模式正在建造", "您可以在测试服务器中做出贡献", "详细信息 请联系服务器管理员"]
Server.listenEvent(ServerListPingEvent.class) {
    serverIcon = Bukkit.loadServerIcon(new File("logo.png"))
    def count = map.getOrDefault(address, 0)
    String speech
    if (count < speeches.size()) {
        speech = speeches[count]
        map.put(address as InetAddress, count + 1)
    } else {
        speech = "现在是" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date())
        map.put(address as InetAddress, 0)
    }
    motd = ChatColor.BLUE.toString() + "OpenCraft " + TextUtil.INSTANCE.getColoredText("Stable\n", TextUtil.TextColor.GOLD, false, true) + TextUtil.INSTANCE.info(speech)
}