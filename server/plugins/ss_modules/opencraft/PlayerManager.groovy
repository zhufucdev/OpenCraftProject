package opencraft

import com.zhufu.opencraft.Info
import com.zhufu.opencraft.OfflineInfo
import org.bukkit.entity.Player

class PlayerManager {

    static Info getInfo(Player player) {
        return com.zhufu.opencraft.PlayerManager.INSTANCE.findInfoByPlayer(player)
    }

    static Info getInfo(UUID uuid) {
        return com.zhufu.opencraft.PlayerManager.INSTANCE.findInfoByPlayer(uuid)
    }

    static OfflineInfo getOfflineInfo(UUID uuid) {
        return com.zhufu.opencraft.PlayerManager.INSTANCE.findOfflineInfoByPlayer(uuid)
    }

    static OfflineInfo getOfflineInfo(String name) {
        return com.zhufu.opencraft.PlayerManager.INSTANCE.findOfflineInfoByName(name)
    }
}
