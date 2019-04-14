package com.zhufu.opencraft

import com.zhufu.opencraft.Base.Extend.isDigit

object TextUtil {
    const val KEY = '§'
    const val END = "§r"

    enum class TextColor {
        BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, GOLD, GREY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE,
        BOLD, UNDERLINED, END, RANDOM, DEL, ITALIC;

        fun getCode(): String = getCode(this)

        companion object {
            fun getCode(color: TextColor): String {
                val sb = StringBuilder(KEY.toString())
                when (color) {
                    BLACK -> sb.append('0')
                    DARK_BLUE -> sb.append('1')
                    DARK_GREEN -> sb.append('2')
                    DARK_AQUA -> sb.append('3')
                    DARK_RED -> sb.append('4')
                    DARK_PURPLE -> sb.append('5')
                    GOLD -> sb.append('6')
                    GREY -> sb.append('7')
                    DARK_GRAY -> sb.append('8')
                    BLUE -> sb.append('9')
                    GREEN -> sb.append('a')
                    AQUA -> sb.append('b')
                    RED -> sb.append('c')
                    LIGHT_PURPLE -> sb.append('d')
                    YELLOW -> sb.append('e')
                    WHITE -> sb.append('f')
                    BOLD -> sb.append('l')
                    UNDERLINED -> sb.append('n')
                    END -> sb.append('r')
                    RANDOM -> sb.append('k')
                    DEL -> sb.append('m')
                    ITALIC -> sb.append('o')
                }
                return sb.toString()
            }
        }
    }

    fun getColoredText(t: String, color: TextColor, bold: Boolean = false, underlined: Boolean = false): String {
        val sb = StringBuilder(TextUtil.TextColor.getCode(color))
        if (bold) {
            sb.append("§l")
        }
        if (underlined) {
            sb.append("§n")
        }
        sb.append(t)
        return sb.toString()
    }

    fun getCustomizedText(t: String, showTo: ChatInfo? = null): String {
        var result = t
        TextUtil.TextColor.values().forEach {
            result = result.replace("\$${it.name}", it.getCode(), true)
        }
        var index = result.indexOf("\${")
        while (index != -1) {
            val end = result.indexOf("}", index)
            if (end == -1) {
                continue
            }
            val value = result.substring(index + 2, end)
            result = result.replaceRange(
                index,
                end + 1,
                if (showTo == null) Language.getDefault(value) else Language.byChat(showTo, value)
            )
            index = result.indexOf("\${")
        }
        return result
    }

    fun error(t: String): String {
        return getColoredText(t, TextUtil.TextColor.RED, true, underlined = false)
    }

    fun info(t: String): String {
        return getColoredText(t, TextUtil.TextColor.YELLOW, true, underlined = false)
    }

    fun tip(t: String): String {
        return getColoredText(t, TextUtil.TextColor.GOLD, false, underlined = true)
    }

    fun success(t: String): String {
        return getColoredText(t, TextUtil.TextColor.GREEN, false, underlined = false)
    }

    fun warn(t: String): String {
        return getColoredText(t, TextUtil.TextColor.RED, false, underlined = true)
    }

    fun printException(e: Exception): Array<String> {
        val list = ArrayList<String>()

        list.add(error("抱歉 服务器出现问题"))
        list.add(getColoredText("----------------------------", TextUtil.TextColor.GOLD, false, false))

        list.add(tip("请将以下信息反映给服务器管理员:"))
        list.add(e.localizedMessage)
        list.add(e.stackTrace.joinToString { "at ${it.className}.${it.methodName}(${TextUtil.TextColor.BLUE.getCode()}${it.fileName}: ${TextUtil.TextColor.UNDERLINED.getCode()}${it.lineNumber})$END" })

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

    private fun Char.isEnglishLetter() = this in 'a'..'Z'
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
            if (string.contains('.')) {
                TextUtil.StringDetectResult.Double
            } else if (string.toLong() > Int.MAX_VALUE) {
                TextUtil.StringDetectResult.Long
            } else TextUtil.StringDetectResult.Int
        } else TextUtil.StringDetectResult.String
    }
}