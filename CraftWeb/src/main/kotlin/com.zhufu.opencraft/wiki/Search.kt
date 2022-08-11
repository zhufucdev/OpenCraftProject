package com.zhufu.opencraft.wiki

import com.google.gson.JsonObject
import com.zhufu.opencraft.util.isEnglishLetter
import kotlin.math.abs

class Search {
    class Condition(
        val keywords: List<String>,
        val tag: List<String> = emptyList(),
        val type: String? = "any",
        val weight: Float = 1F,
        val backup: String = "not"
    )

    class SearchResult(val title: String, val info: JsonObject, val confidence: Float, val keywords: List<String>)

    private val conditions = ArrayList<Condition>()
    lateinit var results: List<SearchResult>
        private set

    private fun Pair<String, String>.howSimilar(): Float {
        if (first.isEmpty() || second.isEmpty()) return 0F

        var match = 1F
        var lastWordAppear = -1
        val longer = if (first.length > second.length) first else second
        val shorter = if (first.length > second.length) second else first
        if (shorter.all { it.isEnglishLetter() }) {
            return if (longer.contains(shorter, true)) 1F else 0F
        }

        shorter.forEach {
            val index = longer.indexOf(it, lastWordAppear, true)
            if (index >= 0) {
                if (lastWordAppear >= 0) {
                    match -= (index - lastWordAppear) / longer.length / shorter.length
                }
                lastWordAppear = index
            } else {
                match -= 1F / shorter.length
            }
        }
        return match
    }

    fun addCondition(condition: Condition) = conditions.add(condition)
    fun doSearch(): List<SearchResult> {
        val finalResults = arrayListOf<SearchResult>()
        val allWeight = conditions.sumByDouble { it.weight.toDouble() }.toFloat()

        Wiki.forEach { info, path ->
            val title = info["isImage"]?.asBoolean?.let { if (!it) info["title"]?.asString ?: return@forEach else path }
                ?: return@forEach
            val fileTag = arrayListOf<String>()

            val result = hashMapOf<Condition, Pair<Float, List<String>>>()
            fun howMatchTag(a: String): Float {
                var r = 0F
                fileTag.forEach {
                    r += (a to it).howSimilar()
                }
                return r
            }
            info["tag"]?.asJsonArray?.forEach {
                fileTag.add(it.asString)
            }
            conditions.forEach { cond ->
                var match = 0F
                val keywords = arrayListOf<String>()
                fun addKeyword(key: String) {
                    if (!keywords.contains(key))
                        keywords.add(key)
                }
                // Keyword Filter
                cond.keywords.forEach { keyword ->
                    match += ((keyword to title).howSimilar() / cond.keywords.size + howMatchTag(keyword) * 0.7F).also {
                        if (it > 0.5F) addKeyword(keyword)
                    }
                }
                if (info.has("subtitle")) {
                    val subtitle = info["subtitle"].asString
                    cond.keywords.forEach { keyword ->
                        match += (((keyword to subtitle).howSimilar() / cond.keywords.size) * 0.85F).also {
                            if (it > 0.5F) addKeyword(keyword)
                        }
                    }
                }
                // Type Filter
                if (cond.type == "image" && !info["isImage"].asBoolean) {
                    match -= 0.3F
                }
                // Tag Filter
                if (cond.tag.isNotEmpty()) {
                    var matchTitle = 0F
                    var matchTag = 0F
                    cond.tag.forEach { tag ->
                        var delta = 0F
                        matchTitle += ((title to tag).howSimilar() / cond.tag.size).also { delta += it }
                        matchTag += howMatchTag(tag).also { delta += it }
                        if (delta > 0.7F) addKeyword(tag)
                    }
                    match += (matchTag + matchTitle * 0.7F)
                }
                // History Filter
                if (cond.backup == "not") {
                    if (info.has("backup"))
                        return@forEach
                } else {
                    val timeTarget = Wiki.backupTimeFormatter.parse(cond.backup)
                    val timeInfo = Wiki.backupTimeFormatter.parse(info["backup"].asString)
                    match -= (abs(timeInfo.time - timeTarget.time) / 10 / 24 / 60 / 60) / 1000F
                }
                result[cond] = match to keywords
            }


            var confidence = 0F
            val keywords = arrayListOf<String>()
            result.forEach { (key, value) ->
                confidence += value.first * key.weight / allWeight
                keywords.addAll(value.second)
            }
            confidence /= conditions.size
            finalResults.add(
                SearchResult(
                    title = path,
                    confidence = confidence,
                    keywords = keywords,
                    info = info
                )
            )
        }


        this.results = finalResults
        return finalResults
    }
}