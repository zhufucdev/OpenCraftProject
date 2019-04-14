package com.zhufu.opencraft

import com.zhufu.opencraft.Translate.DetectResult.*
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.plugin.java.JavaPlugin

class OpenTranslator : JavaPlugin(), Listener, PluginBase {

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        Translator.handler = { msg, info ->
            translate(msg, info)
        }
    }

    private fun translate(msg: String, info: ChatInfo): Boolean {
        if (info.doNotTranslate)
            return false

        val getter = getLangGetter(info)

        val lang = Translate.detectLanguage(msg)
        val detections = Translate.toTranslateOrNot(msg)
        val sb = StringBuilder()
        sb.append(getter["translator.usingLang", lang])
        detections.forEach {
            when (it) {
                Expression -> sb.append(getter["translator.containsExpr"])
                ColorCode -> sb.append(getter["translator.containsColor"])
                LanguageExpr -> sb.append(getter["translator.containsLang"])
            }
            sb.append(',')
        }
        sb.deleteCharAt(sb.lastIndex)
        info.playerStream.send(TextUtil.info(sb.toString()))
        info.playerStream.send(getter["translator.translating"])

        val translations = HashMap<String, String>()
        Language.languages.forEach {
            val target = it.getString("info.code")!!
            val translation =
                    if (target != lang || detections.isNotEmpty())
                        try {
                            Translate.translate(msg, target = target)
                        } catch (e: Exception) {
                            e.printStackTrace();"(CouldNotTranslate)$msg"
                        }
                    else TextUtil.getCustomizedText(msg)
            translations[target] = translation
        }

        Bukkit.getScheduler().runTask(this) { _ ->
            PlayerManager.forEachChatter {
                val translation = translations[it.targetLang]!!
                it.playerStream.sendChat(info.displayName, msg, translation, emptyList())
            }
        }
        Bukkit.getConsoleSender().sendMessage("<${info.displayName}(已翻译)> $msg")
        return true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerAsyncChat(event: AsyncPlayerChatEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (info == null) {
            event.player.error(Language.getDefault("player.error.unknown"))
            return
        }
        event.isCancelled = Translator.chat(event.message, info)
    }
}