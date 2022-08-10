package com.zhufu.opencraft.survey

import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.zhufu.opencraft.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.TimeoutException
import kotlin.concurrent.timer

object SurveyManager : Listener {
    enum class QuestionType {
        SingleChoice, MultiChoice, Text;

        fun getChineseName(): String {
            return when (this) {
                SingleChoice -> "单选"
                MultiChoice -> "多选"
                Text -> "问答"
            }
        }
    }

    class Question(
        val content: String,
        val type: QuestionType,
        val answer: String,
        val selections: MutableList<String> = mutableListOf()
    ) {
        var playerAnswer: String = ""

        val isAnswerVaild: Boolean
            get() {
                when (type) {
                    QuestionType.SingleChoice -> {
                        return playerAnswer.length == 1 && playerAnswer.first().isLetter()
                    }
                    QuestionType.MultiChoice -> {
                        var r = true
                        playerAnswer.forEach {
                            if (!it.isLetter() && it != ',') {
                                return false
                            }
                        }
                        return true
                    }
                    QuestionType.Text -> {
                        return playerAnswer.isNotEmpty()
                    }
                }
            }

        val isCorrect: Boolean
            get() =
                if (!isAnswerVaild)
                    false
                else if (type == QuestionType.Text) {
                    val answers = answer.split('|')
                    answers.contains(playerAnswer)
                } else if (type == QuestionType.MultiChoice) {
                    val selections = answer.split(',')
                    val split = this.playerAnswer.split(',')
                    val playerAnswerer = ArrayList<String>()
                    split.forEach { playerAnswerer.add(it.toUpperCase()) }

                    var r = true
                    for (it in selections)
                        if (!playerAnswer.contains(it.toUpperCase())) {
                            r = false
                            break
                        }
                    r
                } else playerAnswer.toUpperCase() == answer.toUpperCase()

        override fun equals(other: Any?): Boolean {
            return other is Question
                    && other.content == this.content
                    && other.answer == this.answer
                    && other.type == this.type
                    && other.selections.containsAll(this.selections)
        }

        override fun hashCode(): Int {
            var result = content.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + answer.hashCode()
            result = 31 * result + selections.hashCode()
            return result
        }
    }

    class PlayerAnswerer(val player: Player) {
        var index = -1
        val currentQuestion: Question
            get() = question[index]
        val hasNext: Boolean
            get() = index + 1 < Game.env.getInt("countOfSurveyQuestion")

        fun nextQuestion() {
            index++
            sendQuestion()
        }

        fun periodQuestion() {
            index--
            sendQuestion()
        }

        private fun formatLine(hor: Int, args: MutableList<String>): String {
            var max = args.first().length
            for (i in 1 until args.size) {
                if (args[i].length > max) {
                    max = args[i].length
                }
            }
            max += 4

            val sb = StringBuilder()
            fun space(i: Int) {
                (0 until i).forEach { sb.append(' ') }
            }
            args.forEachIndexed { index, s ->
                sb.append(s)
                space(max - s.length)
                if ((index + 1) % hor == 0)
                    sb.append(System.lineSeparator())
            }
            return sb.toString()
        }

        private fun sendQuestion() {
            val question = question[index]
            player.sendMessage(
                TextUtil.getColoredText(
                    "${index + 1}.[${question.type.getChineseName()}] Q: ",
                    TextUtil.TextColor.BLUE,
                    bold = false,
                    underlined = false
                ) + question.content
            )
            fun sendSelections() {
                player.sendMessage(formatLine(2, question.selections))
            }
            if (question.type == QuestionType.MultiChoice) {
                sendSelections()
                player.sendMessage(TextUtil.tip("提示: 多选题请使用 选项,选项... 的格式，例如: A,B,C,D，否则一律判为错误"))
            } else if (question.type == QuestionType.SingleChoice) {
                sendSelections()
                player.sendMessage(TextUtil.tip("提示: 单选题只能输入一个英文字母，大小写皆可，否则一律判为错误"))
            }
        }

        val question = ArrayList<Question>()

        val isPassed: Boolean
            get() {
                var r = true
                question.forEach {
                    if (!it.isCorrect) {
                        r = false
                        player.sendMessage(
                            TextUtil.error("您所答题目")
                                    + "\"${it.content}\""
                                    + TextUtil.error("的答案是错误的")
                                    + TextUtil.tip("正确答案应为:${it.answer}")
                        )
                    }
                }
                if (r) {
                    player.sendMessage(
                        TextUtil.getColoredText(
                            "恭喜，您的回答全部正确，您现在已经成为正式会员了",
                            TextUtil.TextColor.GOLD,
                            true,
                            false
                        )
                    )
                }
                return r
            }

        init {
            val randoms =
                MutableList(Game.env.getInt("countOfSurveyQuestion")) { Base.random.nextInt(questionList.size) }
            val start = System.currentTimeMillis()
            for (i in 0 until randoms.size) {
                if (i != 0) {
                    val unless = ArrayList<Int>()
                    for (j in i - 1 downTo 0) {
                        while (randoms[j] == randoms[i] || unless.contains(randoms[i])) {
                            randoms[i] = Base.random.nextInt(questionList.size)
                            if (System.currentTimeMillis() - start >= 3 * 1000L) {
                                throw TimeoutException()
                            }
                        }
                        unless.add(randoms[j])
                    }
                }
                question.add(questionList[randoms[i]])
            }
            nextQuestion()
        }
    }

    val questionList = ArrayList<Question>()
    private val testMap = HashMap<Player, PlayerAnswerer>()
    fun init(surveySave: File, plugin: JavaPlugin?) {
        if (!surveySave.exists()) {
            plugin?.logger?.warning("Survey file $surveySave doesn't exist.")
            return
        }
        try {
            val reader = JsonReader(surveySave.reader())
            val parser = JsonParser()
            reader.beginArray()
            while (reader.hasNext()) {
                val obj = parser.parse(reader).asJsonObject

                if (!obj.has("Content")
                    || !obj.has("Type")
                    || !obj.has("Answer")
                    || (obj["Type"].asString.contains("Choice") && !obj.has("Selections"))
                ) {
                    Bukkit.getLogger().warning("[SurveyManager] No enough args for object ${reader.path}")
                    continue
                }
                val content = obj["Content"].asString
                val type = QuestionType.valueOf(obj["Type"].asString)
                val answer = obj["Answer"].asString
                val selections = ArrayList<String>()
                if (type == QuestionType.SingleChoice || type == QuestionType.MultiChoice)
                    obj["Selections"].asJsonArray.forEach { selections.add(it.asString) }
                questionList.add(Question(content, type, answer, selections))
            }
            reader.endArray()
            reader.close()
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
        if (plugin != null)
            Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun startSurvey(player: Player) {
        if (testMap.containsKey(player)) {
            player.sendMessage(TextUtil.info("您已经开始答题了"))
            return
        }
        val info = PlayerManager.findInfoByPlayer(player)
        if (info == null) {
            player.error(Language.getDefault("player.error.unknown"))
            return
        }
        if (info.isSurveyPassed) {
            player.sendMessage(TextUtil.info("您已经成为正式会员，不需要再答题"))
            return
        }
        if (info.remainingSurveyChance > 0) {
            info.doNotTranslate = true
            try {
                val answerer = PlayerAnswerer(player)
                testMap[player] = answerer
                val limit = answerer.question.size * Game.env.getInt("secondsPerQuestion") * 1000L
                val delay = 10 * 1000L
                var i = 1
                val iLimit = limit / delay
                timer("surveyTimer", initialDelay = delay, period = delay) {
                    if (!testMap.containsKey(player))
                        this.cancel()
                    if (iLimit > i) {
                        player.sendTitle(TextUtil.info("您还剩${(iLimit - i) * 10}秒"), "", 7, 40, 7)
                    } else {
                        player.sendTitle(TextUtil.tip("时间到!"), "", 7, 40, 7)
                        exit(player)
                        this.cancel()
                    }
                    i++
                }
                player.sendTitle(TextUtil.tip("您将开始答题"), TextUtil.info("同时，您的聊天已被禁用"), 7, 70, 7)
                player.sendMessage(TextUtil.info("使用键盘在聊天框中答题"))
                player.sendMessage(TextUtil.tip("您可以通过输入\"period\"来返回上一题，或是\"exit\"来退出答题(您的进度将不会被保存)"))
            } catch (e: TimeoutException) {
                e.printStackTrace()
                player.error("获取问卷超时")
            }
        } else {
            player.sendMessage(TextUtil.error("抱歉，但您参加调查的机会已用尽，您可以尝试请求管理员的帮助"))
        }
    }

    private fun exit(player: Player) {
        testMap.remove(player)
        player.sendMessage(TextUtil.info("您已退出答题"))
        val info = PlayerManager.findInfoByPlayer(player) ?: return
        info.doNotTranslate = true

        info.remainingSurveyChance--
        player.sendMessage("您还剩${info.remainingSurveyChance}次机会")
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerAsyncChat(event: AsyncPlayerChatEvent) {
        val answerer = testMap[event.player]
        if (answerer != null) {
            event.isCancelled = true
            if (event.message == "period") {
                if (answerer.index > 0) {
                    answerer.periodQuestion()
                } else {
                    event.player.sendMessage(TextUtil.error("当前题目是第一题"))
                }
            } else if (event.message == "exit") {
                exit(event.player)
            } else {
                answerer.currentQuestion.playerAnswer = event.message
                if (answerer.hasNext)
                    answerer.nextQuestion()
                else {
                    val info = PlayerManager.findInfoByPlayer(event.player)
                    if (info == null) {
                        event.player.error(Language.getDefault("player.error.unknown"))
                    } else {
                        info.isSurveyPassed = answerer.isPassed
                            .also {
                                if (!it) {
                                    info.remainingSurveyChance--
                                    event.player.sendMessage("您还剩${info.remainingSurveyChance}次机会")
                                }
                            }
                        testMap.remove(event.player)
                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (testMap.containsKey(event.player)) {
            exit(event.player)
        }
    }
}