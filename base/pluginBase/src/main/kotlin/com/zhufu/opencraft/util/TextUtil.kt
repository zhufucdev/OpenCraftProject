package com.zhufu.opencraft.util

import com.zhufu.opencraft.Base.Extend.isDigit
import com.zhufu.opencraft.api.ChatInfo
import com.zhufu.opencraft.getter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration

object TextUtil {
    const val KEY = '§'
    const val END = "§r"

    enum class TextColor(val code: String) {
        BLACK("${KEY}0"), DARK_BLUE("${KEY}1"), DARK_GREEN("${KEY}2"), DARK_AQUA("${KEY}3"),
        DARK_RED("${KEY}4"), DARK_PURPLE("${KEY}5"), GOLD("${KEY}6"), GREY("${KEY}7"),
        DARK_GRAY("${KEY}8"), BLUE("${KEY}9"), GREEN("${KEY}a"), AQUA("${KEY}b"), RED("${KEY}c"),
        LIGHT_PURPLE("${KEY}d"), YELLOW("${KEY}e"), WHITE("${KEY}f"),
        BOLD("${KEY}l"), UNDERLINED("${KEY}n"), END("${KEY}r"), RANDOM("${KEY}k"), DEL("${KEY}m"),
        ITALIC("${KEY}o"),
        INFO("$YELLOW"), ERROR("$BOLD$RED"), SUCCESS("$GREEN"), WARN("${BOLD}${GOLD}"),
        TIP("$BOLD$GOLD");

        override fun toString(): String = code
    }

    val INFO_STYLE get() = Style.style(NamedTextColor.YELLOW)
    val SUCCESS_STYLE get() = Style.style(NamedTextColor.GREEN)
    val ERROR_STYLE get() = Style.style(NamedTextColor.RED, TextDecoration.BOLD)
    val WARN_STYLE get() = Style.style(NamedTextColor.GOLD, TextDecoration.UNDERLINED)
    val TIP_STYLE get() = Style.style(NamedTextColor.GOLD, TextDecoration.BOLD)

    fun getColoredText(t: String, color: TextColor, bold: Boolean = false, underlined: Boolean = false): String {
        val sb = StringBuilder(color.code)
        if (bold) {
            sb.append("§l")
        }
        if (underlined) {
            sb.append("§n")
        }
        sb.append(t)
        return sb.toString()
    }

    fun getCustomizedText(t: String, getter: Language.LangGetter): String {
        var result = t
        TextUtil.TextColor.values().forEach {
            result = result.replace("\$${it.name}", it.code, true)
        }
        var index = result.indexOf("\${")
        while (index != -1) {
            val end = result.indexOf("}", index)
            if (end == -1) {
                continue
            }
            var value = result.substring(index + 2, end)

            val args = arrayListOf<String>()
            if (value.contains(',')) {
                val parts = value.split(',')
                value = parts.first()
                for (i in 1 until parts.size) {
                    args.add(parts[i])
                }
            }
            result = result.replaceRange(
                index,
                end + 1,
                Language.got(getter.lang, value, args.toTypedArray())
            )
            index = result.indexOf("\${")
        }
        return result
    }

    fun getCustomizedText(t: String, showTo: ChatInfo? = null): String = getCustomizedText(t, showTo.getter())

    @Deprecated("Outdated", ReplaceWith("toErrorMessage"))
    fun error(t: String): String {
        return getColoredText(t, TextColor.ERROR)
    }
    @Deprecated("Outdated", ReplaceWith("toInfoMessage"))
    fun info(t: String): String {
        return getColoredText(t, TextColor.INFO)
    }
    @Deprecated("Outdated", ReplaceWith("toTipMessage"))
    fun tip(t: String): String {
        return getColoredText(t, TextColor.TIP)
    }
    @Deprecated("Outdated", ReplaceWith("toSuccessMessage"))
    fun success(t: String): String {
        return getColoredText(t, TextColor.SUCCESS)
    }
    @Deprecated("Outdated", ReplaceWith("toWarnMessage"))
    fun warn(t: String): String {
        return getColoredText(t, TextColor.WARN)
    }

    fun printException(e: Exception): Array<String> {
        val list = ArrayList<String>()

        list.add(error("抱歉 服务器出现问题"))
        list.add(getColoredText("----------------------------", TextUtil.TextColor.GOLD, false, false))

        list.add(tip("请将以下信息反映给服务器管理员:"))
        list.add(e.localizedMessage)
        list.add(e.stackTrace.joinToString { "at ${it.className}.${it.methodName}(${TextUtil.TextColor.BLUE.code}${it.fileName}: ${TextUtil.TextColor.UNDERLINED}${it.lineNumber})$END" })

        return list.toTypedArray()
    }

    fun format(content: String, title: String): Array<String> {
        val top = getColoredText("-------$title-------", TextColor.GOLD, true, false)
        val end = getColoredText(">>END", TextColor.GOLD, true, false)

        return arrayOf(top, content, end)
    }

    fun format(milli: Long, lang: Language.LangGetter): String {
        return when {
            milli < 0 -> lang["command.error.unknown"]
            milli < 60 * 1000L -> "${milli / 1000L}${lang["ui.time.sec"]}"
            milli in 60 * 1000L until 60 * 60 * 1000L -> {
                val seconds = milli / 1000L
                "${seconds / 60L}${lang["ui.time.min"]}${seconds - seconds / 60L * 60}${lang["ui.time.sec"]}"
            }
            milli in 60 * 60 * 1000L until 60 * 60 * 60 * 1000L -> {
                val seconds = milli / 1000L
                val minute = seconds / 60L
                "${minute / 60L}${lang["ui.time.hour"]}${minute - minute / 60L * 60}${lang["ui.time.min"]}${seconds - seconds / 60L * 60}${lang["ui.time.sec"]}"
            }
            else -> {
                val seconds = milli / 1000L
                val minute = seconds / 60L
                val hours = minute / 60L
                "${hours / 24}${lang["ui.time.day"]}${hours - hours / 60 * 60}${lang["ui.time.hour"]}${minute - minute / 60 * 60}${lang["ui.time.min"]}${seconds - seconds / 60 * 60}${lang["ui.time.sec"]}"
            }
        }
    }

    fun formatLore(s: String): List<String> {
        val r = ArrayList<String>()
        val border = 15
        if (s.length > border) {
            r.add(s.substring(0, border) + if (s[border - 1].isEnglishLetter()) "-" else "")
            r.addAll(formatLore(s.substring(border)))
        } else r.add(s)
        return r
    }

    enum class StringDetectResult {
        String, Int, Long, Double
    }

    fun detectString(string: String): StringDetectResult {
        return if (string.isDigit()) {
            when {
                string.contains('.') -> TextUtil.StringDetectResult.Double
                string.toLong() > Int.MAX_VALUE -> TextUtil.StringDetectResult.Long
                else -> TextUtil.StringDetectResult.Int
            }
        } else TextUtil.StringDetectResult.String
    }
}

fun String.toInfoMessage() = Component.text(this, TextUtil.INFO_STYLE)
fun String.toTipMessage() = Component.text(this, TextUtil.TIP_STYLE)
fun String.toWarnMessage() = Component.text(this, TextUtil.WARN_STYLE)
fun String.toSuccessMessage() = Component.text(this, TextUtil.SUCCESS_STYLE)
fun String.toErrorMessage() = Component.text(this, TextUtil.ERROR_STYLE)
fun String.toComponent() = Component.text(this)
fun String.toCustomizedMessage(info: ChatInfo?) = TextUtil.getCustomizedText(this, info)
fun String.toCustomizedMessage(getter: Language.LangGetter) = TextUtil.getCustomizedText(this, getter)
fun Char.isEnglishLetter() = this in 'a'..'z' || this in 'A'..'Z'