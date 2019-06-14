package com.zhufu.opencraft

import com.zhufu.opencraft.util.ActionBarTextUtil
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

fun getLang(lang: String, value: String, vararg replaceWith: Any?): String = Language.got(lang, value, replaceWith)
fun getLang(player: ServerPlayer, value: String, vararg replaceWith: Any?): String =
    Language.got(lang = player.userLanguage, value = value, replaceWith = replaceWith)

fun getLang(sender: CommandSender, value: String, vararg replaceWith: Any?): String {
    return if (sender is Player) {
        val info = OfflineInfo.findByUUID(sender.uniqueId)
            ?: return Language.got(Language.LANG_ZH, "player.error.unknown", null)
        Language.got(info.userLanguage, value, replaceWith)
    } else {
        Language.got(Language.default.getString("info.code")!!, value, replaceWith)
    }
}

fun getLangGetter(player: ChatInfo?): Language.LangGetter {
    return if (player != null) {
        Language[player.targetLang]
    } else {
        Language.LangGetter(Language.default)
    }
}

fun HumanEntity.info() = Info.findByPlayer(uniqueId)
fun OfflinePlayer.offlineInfo() = OfflineInfo.findByUUID(uniqueId)
fun CommandSender?.lang() = getLangGetter(if (this is HumanEntity) this.info() else null)
fun ChatInfo?.lang() = getLangGetter(this)

fun CommandSender.success(msg: String) {
    this.sendMessage(TextUtil.success(msg))
}

fun CommandSender.info(msg: String) {
    this.sendMessage(TextUtil.info(msg))
}

fun CommandSender.error(msg: String) {
    this.sendMessage(TextUtil.error(msg))
}

fun CommandSender.tip(msg: String) {
    this.sendMessage(TextUtil.tip(msg))
}

fun CommandSender.warn(msg: String) {
    this.sendMessage(TextUtil.warn(msg))
}

fun Player.sendActionText(msg: String) {
    ActionBarTextUtil.sendActionText(this, msg)
}

fun broadcast(value: String, color: TextUtil.TextColor, vararg replaceWith: String?) {
    val langMap = HashMap<String, String>()
    Bukkit.getOnlinePlayers().forEach {
        val info = OfflineInfo.findByUUID(it.uniqueId) ?: return@forEach
        val lang = info.userLanguage
        if (!langMap.containsKey(lang)) {
            langMap[lang] = TextUtil.getColoredText(Language.got(lang, value, replaceWith), color, false, false)
        }
        it.sendMessage(langMap[lang]!!)
    }
}

fun runSync(l: () -> Unit) {
    Bukkit.getScheduler().runTask(Base.pluginCore) { _ ->
        l()
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : ItemMeta> ItemStack.updateItemMeta(block: T.() -> Unit): ItemStack {
    itemMeta = (itemMeta as T).apply(block)
    return this
}