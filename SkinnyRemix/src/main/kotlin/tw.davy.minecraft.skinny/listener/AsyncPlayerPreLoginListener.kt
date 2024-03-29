package tw.davy.minecraft.skinny.listener

import com.destroystokyo.paper.profile.ProfileProperty
import com.zhufu.opencraft.PlayerManager
import com.zhufu.opencraft.getLang
import com.zhufu.opencraft.util.TextUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent

import tw.davy.minecraft.skinny.Skinny

/**
 * @author Davy
 */
class AsyncPlayerPreLoginListener(private val mPlugin: Skinny) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onAsyncPlayerPreLogin(preLoginEvent: AsyncPlayerPreLoginEvent) {
        if (preLoginEvent.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            return

        val playerName = preLoginEvent.name
        val info = PlayerManager.findOfflineInfoByPlayer(preLoginEvent.uniqueId)
        var skin = Skinny.instance.cache.getOrDefault(playerName, null)
        if (skin == null) {
            val customTarget = info?.skin
            if (customTarget != null) {
                skin = mPlugin.providerManager.getSkin(customTarget)
                skin!!.isCustomize = true
            } else {
                skin = mPlugin.providerManager.getSkin(playerName)
            }
        }

        //Added by zhufucdev for server use.
        if (skin != null) {
            if (skin.isCustomize && info != null)
                MessagePool.instance.sendMessageToPlayer(
                    preLoginEvent.uniqueId,
                    TextUtil.info(getLang(info, "skinny.working"))
                )
            mPlugin.cache[playerName] = skin
            preLoginEvent.playerProfile.setProperty(ProfileProperty("textures", skin.value, skin.signature))
        } else if (info != null) {
            MessagePool.instance.sendMessageToPlayer(
                preLoginEvent.uniqueId,
                TextUtil.error(getLang(info, "skinny.error.load"))
            )
        }
    }
}
