package com.zhufu.opencraft.survey

import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.zhufu.opencraft.*
import com.zhufu.opencraft.util.*
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.time.Duration
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
                    split.forEach { playerAnswerer.add(it.uppercase()) }

                    var r = true
                    for (it in selections)
                        if (!playerAnswer.contains(it.uppercase())) {
                            r = false
                            break
                        }
                    r
                } else playerAnswer.uppercase() == answer.uppercase()

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
                Component.text("${index + 1}.[${question.type.getChineseName()}] Q: ")
                    .color(NamedTextColor.AQUA)
                    .append(Component.text(question.content))
            )
            fun sendSelections() {
                player.sendMessage(formatLine(2, question.selections))
            }
            if (question.type == QuestionType.MultiChoice) {
                sendSelections()
                player.tip("提示: 多选题请使用 选项,选项... 的格式，例如: A,B,C,D，否则一律判为错误")
            } else if (question.type == QuestionType.SingleChoice) {
                sendSelections()
                player.tip("提示: 单选题只能输入一个英文字母，大小写皆可，否则一律判为错误")
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
                            "你所答题目".toErrorMessage()
                                .append("\"${it.content}\"".toComponent())
                                .append("的答案是错误的".toErrorMessage())
                                .append("正确答案应为:${it.answer}".toTipMessage())
                        )
                    }
                }
                if (r) {
                    player.success("恭喜，你的回答全部正确，你现在已经成为正式会员了")
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
            reader.beginArray()
            while (reader.hasNext()) {
                val obj = JsonParser.parseReader(reader).asJsonObject

                if (!obj.has("content")
                    || !obj.has("type")
                    || !obj.has("answer")
                    || (obj["type"].asString.contains("choice") && !obj.has("selections"))
                ) {
                    Bukkit.getLogger().warning("[SurveyManager] No enough args for object ${reader.path}")
                    continue
                }
                val content = obj["content"].asString
                val type = QuestionType.valueOf(obj["type"].asString)
                val answer = obj["answer"].asString
                val selections = ArrayList<String>()
                if (type == QuestionType.SingleChoice || type == QuestionType.MultiChoice)
                    obj["selections"].asJsonArray.forEach { selections.add(it.asString) }
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
            player.info("你已经开始答题了")
            return
        }
        val info = PlayerManager.findInfoByPlayer(player)
        if (info == null) {
            player.error(Language.getDefault("player.error.unknown"))
            return
        }
        if (info.isSurveyPassed) {
            player.info("你已经成为正式会员，不需要再答题")
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
                    fun title(t: Component) {
                        player.showTitle(
                            Title.title(
                                t,
                                Component.empty(),
                                Title.Times.times(
                                    Duration.ofMillis(150),
                                    Duration.ofSeconds(2),
                                    Duration.ofMillis(150)
                                )
                            )
                        )
                    }
                    if (iLimit > i) {
                        title("你还剩${(iLimit - i) * 10}秒".toInfoMessage())
                    } else {
                        title("时间到!".toTipMessage())
                        exit(player)
                        this.cancel()
                    }
                    i++
                }
                player.showTitle(
                    Title.title(
                        "你将开始答题".toTipMessage(),
                        "同时，你的聊天已被禁用".toInfoMessage(),
                        Title.Times.times(
                            Duration.ofMillis(300),
                            Duration.ofSeconds(4),
                            Duration.ofMillis(150)
                        )
                    )
                )
                player.info("使用键盘在聊天框中答题")
                player.tip("你可以通过输入\"period\"来返回上一题，或是\"exit\"来退出答题(你的进度将不会被保存)")
            } catch (e: TimeoutException) {
                e.printStackTrace()
                player.error("获取问卷超时")
            }
        } else {
            player.error("抱歉，但你参加调查的机会已用尽，你可以尝试请求管理员的帮助")
        }
    }

    private fun exit(player: Player) {
        testMap.remove(player)
        player.info("你已退出答题")
        val info = PlayerManager.findInfoByPlayer(player) ?: return
        info.doNotTranslate = true

        info.remainingSurveyChance--
        player.sendMessage("你还剩${info.remainingSurveyChance}次机会")
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerAsyncChat(event: AsyncChatEvent) {
        val answerer = testMap[event.player]
        if (answerer != null) {
            event.isCancelled = true
            val msg = (event.message() as TextComponent).content()
            if (msg == "period") {
                if (answerer.index > 0) {
                    answerer.periodQuestion()
                } else {
                    event.player.error("当前题目是第一题")
                }
            } else if (msg == "exit") {
                exit(event.player)
            } else {
                answerer.currentQuestion.playerAnswer = msg
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
                                    event.player.sendMessage("你还剩${info.remainingSurveyChance}次机会")
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