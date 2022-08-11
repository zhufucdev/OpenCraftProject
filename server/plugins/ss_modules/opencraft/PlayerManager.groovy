package opencraft

import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.data.OfflineInfo
import org.bukkit.entity.Player

class PlayerManager {
    private static final INSTANCE = com.zhufu.opencraft.PlayerManager.INSTANCE
    static Info getInfo(Player player) {
        return INSTANCE.findInfoByPlayer(player)
    }

    static Info getInfo(UUID uuid) {
        return INSTANCE.findInfoByPlayer(uuid)
    }

    static OfflineInfo getOfflineInfo(UUID uuid) {
        return INSTANCE.findOfflineInfoByPlayer(uuid)
    }

    static OfflineInfo getOfflineInfo(String name) {
        return INSTANCE.findOfflineInfoByName(name)
    }
}
