package tw.davy.minecraft.skinny.listener

import com.comphenix.protocol.wrappers.WrappedGameProfile
import com.comphenix.protocol.wrappers.WrappedSignedProperty
import com.google.common.collect.Multimap

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent

import tw.davy.minecraft.skinny.SignedSkin
import tw.davy.minecraft.skinny.Skinny

/**
 * @author Davy
 */
class PlayerLoginListener(private val mPlugin: Skinny) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerLogin(loginEvent: PlayerLoginEvent) {
        if (loginEvent.result != PlayerLoginEvent.Result.ALLOWED)
            return

        val player = loginEvent.player
        val skin = mPlugin.cache.getOrDefault(player.name, null) ?: return

        val profile = WrappedGameProfile.fromPlayer(player)
        val properties = profile.properties
        properties.clear()
        Thread {
            properties.put("textures",
                    WrappedSignedProperty.fromValues("textures", skin.value, skin.signature))
        }.start()
    }
}
