package com.zhufu.opencraft

import com.zhufu.opencraft.Translate.DetectResult.*
import com.zhufu.opencraft.api.ChatInfo
import com.zhufu.opencraft.data.ServerPlayer
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toInfoMessage
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.plugin.java.JavaPlugin

class OpenTranslator : JavaPlugin(), Listener {

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

        val detectLang = Translate.detectLanguage(msg)
        val detections = Translate.toTranslateOrNot(msg)
        val sb = StringBuilder()
        sb.append(getter["translator.usingLang", detectLang])
        detections.map {
            when (it) {
                Expression -> sb.append(getter["translator.containsExpr"])
                ColorCode -> sb.append(getter["translator.containsColor"])
                LanguageExpr -> sb.append(getter["translator.containsLang"])
            }
            sb.append(' ')
        }
        sb.deleteCharAt(sb.lastIndex)
        info.playerOutputStream.send(sb.toString().toInfoMessage())
        info.playerOutputStream.send(getter["translator.translating"])

        val translations = HashMap<String, String>()
        fun addTranslation(target: String): String {
            val translation =
                if (target != detectLang || detections.isNotEmpty())
                    try {
                        Translate.translate(msg, target = target)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        "(CouldNotTranslate)$msg"
                    }
                else TextUtil.getCustomizedText(msg)
            translations[target] = translation
            return translation
        }

        PlayerManager.forEachChatter {
            if (it is ServerPlayer && !it.preference.showTranslations) {
                Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
                    it.playerOutputStream.sendChat(info, msg)
                }
            } else {
                val translation =
                    if (translations.containsKey(it.targetLang)) translations[it.targetLang]!!
                    else addTranslation(it.targetLang)
                Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
                    try {
                        if (it.targetLang == detectLang && detections.isEmpty())
                            it.playerOutputStream.sendChat(info, msg)
                        else
                            it.playerOutputStream.sendChat(info, msg, translation, emptyList())
                    } catch (ignore: Exception) {

                    }
                }
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
        if (!info.doNotTranslate) {
            event.isCancelled = true
            Translator.chat(event.message, info)
        }
    }
}