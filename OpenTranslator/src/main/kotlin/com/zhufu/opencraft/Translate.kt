package com.zhufu.opencraft

import com.baidu.translate.LangDetect
import com.baidu.translate.TransApi
import com.google.gson.JsonParser
import org.bukkit.entity.HumanEntity
import parsii.eval.Parser

object Translate {
    private const val appID = "20180812000193597"
    private const val key = "xiD6QPfbdUhzjbX9sWVe"
    const val splitKeyword = "!*"
    const val exprKeyword = '^'

    private val translate = TransApi(appID, key)

    class TranslateInfo(val key: String, val translatable: Boolean) {
        val isExpr
            get() = key.contains("$exprKeyword(") && key.length > 1
    }

    fun translate(source: String, from: String = "auto", target: String): String {
        val toTranslate = ArrayList<TranslateInfo>()

        var result = source
        if (source.contains('$')) {
            TextUtil.TextColor.values().forEach {
                result = result.replace("\$${it.name}", "!*${it.code}!*", true)
            }

            var index = result.indexOf("\${")
            while (index != -1) {
                val end = result.indexOf("}", index)
                if (end == -1) {
                    continue
                }
                val value = result.substring(index, end + 1)
                result = result.replaceRange(
                    index,
                    end + 1,
                    "!*${TextUtil.getCustomizedText(value, Language.LangGetter(target))}!*"
                )
                index = result.indexOf("\${", end)
            }
        }

        if (result.contains(splitKeyword)) {
            result.split(splitKeyword).forEachIndexed { index: Int, s: String ->
                toTranslate.add(TranslateInfo(s, index % 2 == 0))
            }
        } else {
            toTranslate.add(TranslateInfo(result, true))
        }

        val translations = StringBuilder()
        fun doTranslating(text: String) {
            val jsonRaw = translate.getTransResult(text, from, target)
            if (jsonRaw == null) {
                translations.append("(null)")
                return
            }
            val json = JsonParser.parseString(jsonRaw).asJsonObject
            val translation = try {
                json["trans_result"].asJsonArray.first().asJsonObject["dst"].asString
            } catch (e: Exception) {
                null
            }
            if (translation == null) {
                translations.append("(null)")
            } else {
                translations.append(translation)
            }
        }

        toTranslate.forEach {
            if (it.key.isEmpty())
                return@forEach
            if (it.translatable && !it.isExpr) {
                doTranslating(it.key)
            } else if (it.translatable && it.isExpr) {
                val a = it.key.indexOf("$exprKeyword(")
                var iA = 0
                var iB = 0
                var b = -1
                for (i in a until it.key.length) {
                    val c = it.key[i]
                    if (c == '(')
                        iA++
                    else if (c == ')')
                        iB++
                    if (iA == iB && iA != 0) {
                        b = i
                        break
                    }
                }
                if (b == -1) {
                    translations.append("(Unexpected brackets)")
                } else {
                    val expr = it.key.substring(a + 2, b)
                    var exception = ""
                    val expression = try {
                        Parser.parse(expr)
                    } catch (e: Exception) {
                        exception = e.localizedMessage;null
                    }
                    val r = StringBuilder(it.key)
                    r.replace(
                        a, b + 1,
                        expression?.evaluate()?.toString() ?: "($exception)"
                    )
                    if (exception.isEmpty())
                        doTranslating(r.toString())
                    else translations.append(r)
                }
            } else {
                translations.append(it.key)
            }
        }
        return translations.toString()
    }

    fun detectLanguage(text: String): String {
        val raw = LangDetect.detectLanguage(text)
        if (raw == LangDetect.langUnknown) {
            return LangDetect.langUnknown
        }
        val json = JsonParser().parse(raw).asJsonObject
        if (json["error"].asInt != 0)
            return LangDetect.langUnknown
        return json["lan"].asString
    }

    enum class DetectResult {
        Expression, ColorCode, LanguageExpr
    }

    fun toTranslateOrNot(text: String): List<DetectResult> {
        val r = ArrayList<DetectResult>()
        if (text.contains(exprKeyword)) r.add(DetectResult.Expression)
        if (TextUtil.TextColor.values().any { text.contains("\$${it.name}", true) }) r.add(DetectResult.ColorCode)
        if (text.contains("\${") && text.contains("}")) r.add(DetectResult.LanguageExpr)
        return r
    }
}