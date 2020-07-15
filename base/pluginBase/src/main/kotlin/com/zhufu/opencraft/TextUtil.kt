package com.zhufu.opencraft

import com.zhufu.opencraft.Base.Extend.isDigit

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
        INFO("$YELLOW"), ERROR("$BOLD$RED"), SUCCESS("$GREEN"), WARN("$UNDERLINED$RED"),
        TIP("$UNDERLINED$GOLD");

        override fun toString(): String = code
    }

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

    fun error(t: String): String {
        return getColoredText(t, TextColor.ERROR)
    }

    fun info(t: String): String {
        return getColoredText(t, TextColor.INFO)
    }

    fun tip(t: String): String {
        return getColoredText(t, TextColor.TIP)
    }

    fun success(t: String): String {
        return getColoredText(t, TextColor.SUCCESS)
    }

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
        val top = getColoredText(">>$title", TextUtil.TextColor.GOLD, true, false)
        val end = getColoredText(">>END", TextUtil.TextColor.GOLD, true, false)

        return arrayOf(top.toString(), content, end)
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
        if (s.length > 29) {
            r.add(s.substring(0, 29) + if (s[28].isEnglishLetter()) "-" else "")
            r.addAll(formatLore(s.substring(29)))
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

fun String.toInfoMessage() = TextUtil.info(this)
fun String.toTipMessage() = TextUtil.tip(this)
fun String.toWarnMessage() = TextUtil.warn(this)
fun String.toSuccessMessage() = TextUtil.success(this)
fun String.toErrorMessage() = TextUtil.error(this)
fun String.toCustomizedMessage(info: ChatInfo?) = TextUtil.getCustomizedText(this, info)
fun String.toCustomizedMessage(getter: Language.LangGetter) = TextUtil.getCustomizedText(this, getter)
fun Char.isEnglishLetter() = this in 'a'..'z' || this in 'A'..'Z'