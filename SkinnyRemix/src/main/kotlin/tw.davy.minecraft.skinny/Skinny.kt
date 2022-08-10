package tw.davy.minecraft.skinny

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zhufu.opencraft.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit
import tw.davy.minecraft.skinny.listener.AsyncPlayerPreLoginListener
import tw.davy.minecraft.skinny.listener.MessagePool
import tw.davy.minecraft.skinny.providers.ProviderManager

/**
 * @author Davy
 *
 * The file is from Davy@github, but doesn't work directly on my server.
 * As a result, I 'remixed' it to make it work.
 * The origin file is under MIT license. See https://github.com/david50407/Skinny/blob/master/LICENSE
 * --zhufucomcom 07/13/2018
 */
class Skinny : JavaPlugin(), Listener {

    lateinit var providerManager: ProviderManager
        private set
    val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, SignedSkin>() {
            @Throws(Exception::class)
            override fun load(playerName: String): SignedSkin {
                throw UnsupportedOperationException("Not supported yet.")
            }
        })
        .asMap()!!

    override fun onEnable() {
        instance = this

        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }

        saveDefaultConfig()
        val enableProviders = config.getStringList("providers")
        providerManager = ProviderManager(enableProviders)

        server.pluginManager.apply {
            registerEvents(AsyncPlayerPreLoginListener(this@Skinny), this@Skinny)
            registerEvents(MessagePool(), this@Skinny)
        }

        ServerCaller["SolvePlayerSkinProfile"] = {
            val profile = it.first() as PlayerProfile
            val name = it[1] as String
            var skin = cache.getOrDefault(name, null)
            if (skin == null) {
                val uuid = profile.id
                val customTarget =
                    (if (uuid != null) OfflineInfo.findByUUID(uuid) else OfflineInfo.findByName(name))?.skin
                if (customTarget != null) {
                    skin = providerManager.getSkin(customTarget)
                    skin!!.isCustomize = true
                } else {
                    skin = providerManager.getSkin(name)
                }
                cache[name] = skin
            }
            profile.setProperty(ProfileProperty("textures", skin.value, skin.signature))
        }
    }

    override fun onDisable() {
        saveConfig()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (command.name == "sc") {
            if (sender !is Player) {
                sender.sendMessage(TextUtil.error("只有玩家才能使用此命令"))
                return true
            }
            val getter = getLangGetter(sender.info())
            if (args.isEmpty()) {
                sender.error(getter["command.error.usage"])
                return false
            }
            val target = args[0]
            if (target.isEmpty()) {
                sender.error(getter["skinny.error.emptyTarget"])
                return false
            }
            Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
                try {
                    val skin = providerManager.getSkin(target)
                    if (skin == null) {
                        sender.error(getter["skinny.error.targetNotFound"])
                    } else {
                        val info = sender.info()
                        if (info == null) {
                            sender.error(getter["player.error.unknown"])
                            return@runTaskAsynchronously
                        }
                        sender.playerProfile.setProperty(ProfileProperty("texture", skin.value, skin.signature))
                        sender.success(getter["skinny.success", target])
                        sender.tip(getter["skinny.tip"])

                        info.skin = if (target != sender.name) {
                            skin.isCustomize = true
                            target
                        } else null
                        cache[sender.name] = skin
                    }
                } catch (e: Exception) {
                    sender.sendMessage(*TextUtil.printException(e))
                }
            }
            return true
        }
        return false
    }

    companion object {
        lateinit var instance: Skinny
    }
}
