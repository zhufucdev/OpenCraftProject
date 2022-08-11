package com.zhufu.opencraft

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.zhufu.opencraft.Base.Extend.isDigit
import com.zhufu.opencraft.Base.lobby
import com.zhufu.opencraft.events.UserLoginEvent
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toComponent
import com.zhufu.opencraft.util.toErrorMessage
import com.zhufu.opencraft.util.toInfoMessage
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*

class BookNoticer : JavaPlugin(), Listener {
    override fun onEnable() {
        initBookEntries()
        server.pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        saveBook()
    }

    @EventHandler
    fun onPlayerLogin(event: UserLoginEvent) {
        event.player.info()
            ?.apply {
                if (!isSurveyPassed && remainingDemoTime <= 0) {
                    PlayerManager.showPlayerOutOfDemoTitle(event.player)
                    return
                }
            }
        Bukkit.getScheduler().runTaskAsynchronously(this) { _ ->
            event.player.info()?.apply {
                if (preference.sendMessagesOnLogin) messagePool.sendUnreadTo(this)
                Base.publicMsgPool.sendUnreadTo(this)
            }
        }
    }

    @EventHandler
    fun onPlayerLobby(event: PlayerTeleportedEvent) {
        if (event.to?.world != lobby)
            return
        Bukkit.getScheduler().runTaskLater(this, Runnable {
            getBook(event.player).show(event.player,true)
        }, 5)
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (command.name == "notice") {
            if (args.isEmpty()) {
                sender.sendMessage("用法错误".toErrorMessage())
                return false
            }
            when (args.first()) {
                "append" -> {
                    if (args.size == 1) return true
                    val sb = StringBuilder()
                    for (i in 1 until args.size) {
                        sb.append(args[i] + ' ')
                    }
                    bookEntries.add(0, BookContener(sb.toString(), System.currentTimeMillis()))
                    sender.sendMessage("已在每日须知中追加该行".toInfoMessage())
                }
                "preview" -> {
                    if (sender !is Player) {
                        sender.sendMessage("只有玩家才能使用此命令".toErrorMessage())
                        return true
                    }
                    try {
                        getBook(null).show(sender)
                    } catch (e: Exception) {
                        sender.sendMessage("${e::class.simpleName}: ${e.localizedMessage}".toErrorMessage())
                        e.printStackTrace()
                    }
                }
                "remove" -> {
                    if (args.size < 2) {
                        sender.sendMessage("用法错误".toErrorMessage())
                        return true
                    }
                    if (!args[1].isDigit() || args[1].toInt() < 0) {
                        sender.sendMessage("${args[1]}不是有效的数字(自然数)")
                        return true
                    }
                    bookEntries.removeAt(args[1].toInt())
                    sender.sendMessage("已删除第${args[1]}行".toInfoMessage())
                }
                "clear" -> {
                    sender.sendMessage("已删除${bookEntries.size}行".toInfoMessage())
                    bookEntries.clear()
                }

                "update" -> {
                    if (!sender.isOp) {
                        sender.sendMessage("您没有权限使用此命令".toErrorMessage())
                        return true
                    }
                    val sb = StringBuilder()
                    for (i in 1 until args.size) {
                        sb.append(args[i] + ' ')
                    }
                    sb.deleteCharAt(sb.lastIndex)

                    server.onlinePlayers.forEach {
                        it.showTitle(
                            Title.title(
                                "服务器正在更新".toInfoMessage(),
                                "这可能需要几十秒".toComponent(),
                                Title.Times.times(
                                    Duration.ofMillis(500),
                                    Duration.ofDays(2),
                                    Duration.ZERO
                                )
                            )
                        )
                    }
                    bookEntries.add(0, BookContener(sb.toString(), System.currentTimeMillis()))
                    server.reload()
                }
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (command.name == "notice") {
            if (args.size == 1) {
                val second = mutableListOf("append", "preview", "remove", "clear", "update")
                return if (args.first().isNotEmpty()) {
                    val result = ArrayList<String>()
                    second.forEach { if (it.startsWith(args.last())) result.add(it) }
                    result
                } else {
                    second
                }
            } else if (args.size == 2) {
                if (args.first() == "remove") {
                    val result = ArrayList<String>()
                    bookEntries.forEachIndexed { index, _ ->
                        result.add(index.toString())
                    }
                    return result
                }
            }
        }
        return mutableListOf()
    }

    private val bookEntries = ArrayList<BookContener>()

    class BookContener(val string: String, val time: Long) {
        val readers = ArrayList<UUID>()
        fun hasRead(player: Player) = readers.any { it == player.uniqueId }
    }

    private fun initBookEntries() {
        val file = File(dataFolder, "books.json")
        if (!file.parentFile.exists())
            file.parentFile.mkdirs()
        if (!file.exists())
            file.createNewFile()
        val reader = JsonReader(file.reader())
        try {
            reader.beginArray()
            while (reader.hasNext()) {
                reader.beginObject()
                var time = -1L
                var string = ""
                val readers = ArrayList<UUID>()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "time" -> time = reader.nextLong()
                        "string" -> string = reader.nextString()
                        "readers" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                readers.add(UUID.fromString(reader.nextString()))
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                bookEntries.add(BookContener(string, time).also { it.readers.addAll(readers) })
                reader.endObject()
            }
            reader.endArray()
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Bukkit.getScheduler().runTaskLater(this, { _ ->
            server.onlinePlayers.forEach {
                getBook(it).show(it)
            }
        }, 50)
    }

    private fun getBook(player: Player? = null): MyBook {
        val format = SimpleDateFormat("yyyy/MM/dd")
        val dateMap = TreeMap<String, ArrayList<BookContener>> { a, b ->
            b.compareTo(a)
        }
        bookEntries.forEach {
            fun add() {
                val date = format.format(Date(it.time))
                if (dateMap.containsKey(date))
                    dateMap[date]!!.add(it)
                else {
                    val list = ArrayList<BookContener>()
                    list.add(it)
                    dateMap[date] = list
                }
            }
            add()
            if (player != null && !it.hasRead(player)) {
                it.readers.add(player.uniqueId)
            }
        }
        val sb = StringBuilder()
        val getter = getLangGetter(player?.info())
        if (dateMap.isEmpty()) {
            sb.append(TextUtil.tip(getter["book.empty"]))
        } else {
            val lineSeparator = System.lineSeparator()
            dateMap.forEach { (t, u) ->
                sb.append(">>" + TextUtil.tip(t) + TextUtil.END + lineSeparator)
                u.forEachIndexed { _, it ->
                    sb.append(
                        TextUtil.getCustomizedText(it.string)
                    )
                    sb.append(lineSeparator)
                }
                sb.append(lineSeparator)
            }
        }
        return MyBook.BookBuilder()
            .setAuthor(getter["book.author"])
            .setTitle(getter["book.title"])
            .setContent(sb.toString())
            .build()
    }

    private fun saveBook() {
        val file = File(dataFolder, "books.json")
        if (!file.exists())
            file.createNewFile()
        val writer = JsonWriter(file.writer())
        writer.beginArray()
        bookEntries.forEach {
            val jsonObject = JsonObject()
            jsonObject.addProperty("time", it.time)
            jsonObject.addProperty("string", it.string)
            val readers = JsonArray()
            it.readers.forEach { reader -> readers.add(reader.toString()) }
            jsonObject.add("readers", readers)
            writer.jsonValue(jsonObject.toString())
        }
        writer.endArray()
        writer.flush()
    }
}