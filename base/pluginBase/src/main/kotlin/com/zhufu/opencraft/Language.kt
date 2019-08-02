package com.zhufu.opencraft

import com.destroystokyo.paper.utils.PaperPluginLogger
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.HumanEntity
import java.io.File

object Language {
    val languages = ArrayList<YamlConfiguration>()
    fun printLanguages(player: HumanEntity) {
        player.sendMessage(
            buildString {
                languages.forEachIndexed { index, yamlConfiguration ->
                    append("${index + 1}.")
                    append(yamlConfiguration.getString("info.name"))
                    append(',')
                }
                deleteCharAt(lastIndex)
            }
        )
    }

    fun getCodeByName(name: String) =
        languages.firstOrNull { it.getString("info.name") == name }?.getString("info.code")

    fun getCodeByOrder(i: Int) = (if (languages.size < i) null else languages[i - 1])?.getString("info.code")

    val default: YamlConfiguration
        get() = languages.firstOrNull { it.getBoolean("info.default", false) } ?: languages.firstOrNull()
        ?: throw LanguageNotFoundException("Default language not found!")
    val defaultLangCode get() = default.getString("info.code", LANG_ZH)!!

    init {
        init()
    }

    fun init() {
        languages.clear()
        File("plugins/lang").also { if (!it.exists()) it.mkdirs() }.listFiles()?.forEach {
            println("[DualLang] Loading language file for ${it.name}")
            try {
                if (it.isHidden)
                    throw Exception()
                languages.add(YamlConfiguration.loadConfiguration(it).also { conf ->
                    if (!conf.contains("info.code")) conf.set(
                        "info.code",
                        it.nameWithoutExtension
                    )
                })
            } catch (e: Exception) {
                println("[DualLang] Failed to load ${it.name}")
            }
        }
    }

    const val LANG_ZH = "zh"
    const val LANG_EN = "en"
    fun got(lang: String, value: String, replaceWith: Array<out Any?>?): String {
        val conf = getConf(lang) ?: throw LangNotFoundException(lang)
        val found = conf.isSet(value)
        var raw: String
        if (found) {
            raw = conf.getString(value)!!
            replaceWith?.forEachIndexed { i, any ->
                if (any != null) {
                    val index = "%${i + 1}"
                    raw =
                        if (raw.contains(index)) {
                            raw.replace(index, any.toString())
                        } else {
                            raw.replaceFirst("%s", any.toString())
                        }
                }
            }
        } else {
            raw = "ERROR: ${get(lang, "user.error.translationNotFound", value)}"
            PaperPluginLogger.getGlobal().warning("Translation not found: $value in language $lang")
        }
        return raw
    }

    operator fun get(lang: String, value: String, vararg replaceWith: Any?): String = got(lang, value, replaceWith)
    fun byChat(player: ChatInfo, value: String, vararg replaceWith: Any?) =
        got(lang = player.targetLang, value = value, replaceWith = replaceWith)

    operator fun get(player: OfflineInfo, value: String, vararg replaceWith: Any?): String =
        got(lang = player.userLanguage, value = value, replaceWith = replaceWith)

    operator fun get(player: HumanEntity, value: String, vararg replaceWith: Any?): String {
        val info = OfflineInfo.findByUUID(player.uniqueId)
            ?: return got(LANG_ZH, "player.error.unknown", null)
        return got(info.userLanguage, value, replaceWith)
    }

    fun getDefault(value: String, vararg replaceWith: Any?): String =
        got(default.getString("info.code")!!, value, replaceWith)

    fun getConf(lang: String) = languages.firstOrNull { it.getString("info.code") == lang }
    operator fun get(player: OfflineInfo) =
        if (player.isUserLanguageSelected) LangGetter(player) else LangGetter(default.getString("info.code")!!)

    operator fun get(lang: String) = LangGetter(lang)

    class LangGetter {
        val lang: String
        private val conf: YamlConfiguration

        constructor(lang: String) {
            this.lang = lang
            conf = getConf(lang)
                ?: throw LanguageNotFoundException("Language called $lang not found!")
        }

        constructor(lang: YamlConfiguration) {
            this.lang = lang.getString("info.code", null) ?: lang.name
            conf = lang
        }

        constructor(info: ServerPlayer) : this(info.userLanguage)

        operator fun get(value: String, vararg replaceWith: Any?): String {
            val found = conf.isSet(value)
            var raw: String
            if (found) {
                raw = conf.getString(value)!!
                replaceWith.forEachIndexed { i, any ->
                    if (any != null) {
                        val index = "%${i + 1}"
                        raw =
                            if (raw.contains(index)) {
                                raw.replace(index, any.toString())
                            } else {
                                raw.replaceFirst("%s", any.toString())
                            }
                    }
                }
            } else {
                raw = "ERROR: ${get("user.error.translationNotFound", value)}"
                PaperPluginLogger.getGlobal().warning("Translation not found: $value in language $lang")
            }
            return raw
        }

        override fun equals(other: Any?) = other is LangGetter && other.lang == this.lang
        override fun hashCode(): Int {
            var result = lang.hashCode()
            result = 31 * result + conf.hashCode()
            return result
        }
    }

    class LangNotFoundException(what: String) : Exception("Language called $what must exists!")

    class LanguageNotFoundException : IllegalArgumentException {
        constructor()
        constructor(s: String) : super(s)
    }
}