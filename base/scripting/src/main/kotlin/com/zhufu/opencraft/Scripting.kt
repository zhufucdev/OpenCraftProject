package com.zhufu.opencraft

import com.zhufu.opencraft.headers.PlayerHeader
import com.zhufu.opencraft.headers.PublicHeaders
import com.zhufu.opencraft.headers.ServerHeader
import com.zhufu.opencraft.headers.player_wrap.PlayerSelf
import org.bukkit.Bukkit
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.SourceSection
import org.graalvm.polyglot.Value
import java.io.OutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level

object Scripting {
    private val threadPool = Executors.newCachedThreadPool()
    val version get() = "beta1"
    var timeOut = 5000L
    var id = 0

    private fun buildNewContext(out: OutputStream? = null) = Context.newBuilder()
        .allowHostAccess(true)
        .apply {
            if (out != null) out(out)
        }
        .build()
        .apply {
            with(getBindings("js")) {
                PublicHeaders.members.forEach { putMember(it.first, it.second) }
            }
        }

    // Data
    private val contextMap = HashMap<ServerPlayer, Pair<Context, StringBuilder>>()

    operator fun get(player: ServerPlayer) = contextMap[player]?.second?.toString()
    private lateinit var serverContext: Context
    private val mServerStringContext = StringBuilder()
    val serverStringContext by lazy { mServerStringContext.toString() }
    fun cleanUp() {
        if (::serverContext.isInitialized) serverContext.close()
        contextMap.forEach {
            it.value.first.close()
        }
        PlayerSelf.cache.cleanUp()
    }

    // Run
    enum class Executor {
        Sever, Player, Operator
    }

    fun beforeRun(executor: Executor, out: OutputStream? = null) {
        val output = out ?: System.out
        val getter = if (out is PlayerStream) out.lang else Language.LangGetter(Language.defaultLangCode)
        with(output) {
            fun write(src: String) = write(src.toByteArray())
            write(getter["scripting.toRun.1"].toInfoMessage() + System.lineSeparator())
            write(getter["scripting.toRun.2", version, executor.name].toInfoMessage() + System.lineSeparator())
            write(getter["scripting.toRun.3"] + System.lineSeparator())
            flush()
        }
    }

    // => For server
    fun startNewServerContext(out: OutputStream? = null) {
        if (::serverContext.isInitialized)
            serverContext.close()
        serverContext = buildNewContext(out)
        serverContext.getBindings("js").apply {
            ServerHeader.members.forEach {
                putMember(it.first, it.second)
            }
        }
        mServerStringContext.clear()
    }

    fun evalAsServer(src: String, out: OutputStream? = null): Value? = try {
        if (!::serverContext.isInitialized) {
            startNewServerContext(out)
        }
        threadPool.submit<Value> {
            serverContext.eval("js", src).apply {
                mServerStringContext.append(src)
            }
        }[timeOut, TimeUnit.SECONDS]
    } catch (e: Exception) {
        printException(src, e, out ?: System.out)
        serverContext.close(true)
        null
    }

    fun evalLineAsServer(src: String, out: OutputStream? = null): Value? {
        beforeRun(Scripting.Executor.Sever, out)
        startNewServerContext(out)
        return evalAsServer(src, out)
    }

    // => For players
    fun startNewContextFor(player: ServerPlayer) {
        val sb = StringBuilder()
        contextMap[player] =
            buildNewContext(if (player is ChatInfo) player.playerStream else null).apply {
                with(getBindings("js")) {
                    PlayerHeader(player).members.forEach {
                        putMember(it.first, it.second)
                    }
                    if (player.isOp) {
                        ServerHeader.members.forEach {
                            putMember(it.first, it.second)
                        }
                    }
                }
            } to sb
    }

    val loopExecutions = HashMap<Int, Long>()
    fun evalAs(player: ServerPlayer, src: String, scriptName: String): Value? {
        val logger = player.newLogger(Level.SEVERE)
        val out = if (player is ChatInfo) player.playerStream else logger.stream()
        beforeRun(if (player.isOp) Scripting.Executor.Operator else Scripting.Executor.Player, out)

        var index = contextMap[player]
        if (index == null) {
            startNewContextFor(player)
            index = contextMap[player]!!
        }
        val id = this.id++
        return try {
            checkRunnabilityFor(player, src)
            threadPool.submit<Value> {
                index.first.eval("js", rewriteSrc(src, id, player).also { Bukkit.getLogger().info("${player.name}'s actual execution: $it") }).apply {
                    index.second.append(src)
                    loopExecutions.remove(id)
                }
            }[timeOut, TimeUnit.MILLISECONDS]
        } catch (e: Exception) {
            printException(src, e, out)
            if (player !is ChatInfo)
                logger.save(scriptName)
            index.first.close(true)
            loopExecutions.remove(id)
            null
        }
    }

    fun evalLineAs(player: ServerPlayer, src: String, scriptName: String): Value? {
        startNewContextFor(player)
        return evalAs(player, src, scriptName)
    }

    private fun checkRunnabilityFor(player: ServerPlayer, src: String) {
        val split = src.split('\n', ';')
        if (!player.isOp) {
            split.forEachIndexed { index, s ->
                if (s.contains("Java.")) {
                    throw IllegalAccessError(
                        "At line ${index + 1}, ${getLang(
                            player.userLanguage,
                            "command.error.permission"
                        )}"
                    )
                }
            }
        }
    }

    private fun rewriteSrc(src: String, execID: Int, player: ServerPlayer): String {
        val item = "util.loopBump($execID,'${player.uuid.toString()}');"
        return buildString {
            append(src)
            fun findClose(startIndex: Int, openChar: Char, closeChar: Char, src: String): Int {
                var open = 1
                var close = 0
                for (j in startIndex + 1 until src.length) {
                    when (src[j]) {
                        openChar -> open++
                        closeChar -> close++
                    }
                    if (open == close) {
                        return j
                    }
                }
                return -1
            }
            fun findOpen(startIndex: Int, openChar: Char, closeChar: Char, src: String): Int {
                var open = 0
                var close = 1
                for (j in startIndex - 1 .. 0){
                    when (src[j]) {
                        openChar -> open++
                        closeChar -> close++
                    }
                    if (open == close) {
                        return j
                    }
                }
                return -1
            }

            fun stepTwo(startIndex: Int): Int {
                if (startIndex > lastIndex || this[startIndex] == ' ') {
                    // If there is no closure
                    insert(startIndex.let { if (it > lastIndex) length else it }, item)
                    return lastIndex
                } else if (this[startIndex] == '{') {
                    // If there is a long closure
                    insert(startIndex + 1, item)
                    return startIndex + item.length + 1
                } else {
                    var i = startIndex
                    loop@ while (i < length) {
                        when(this[i]) {
                            '(' -> {
                                // To skip it
                                i = findClose(i,'(',')',this.toString())
                            }
                            '\n' -> {
                                insert(i + 1, '}')
                                insert(startIndex, "{$item")
                                return i
                            }
                            ';' -> {
                                insert(i, '}')
                                insert(startIndex, "{$item")
                                return i
                            }
                            ')' -> {
                                val open = findOpen(i,'(',')',this.toString())
                                if (open <= startIndex){
                                    insert(i,'}')
                                    insert(startIndex,"{$item")
                                }
                                return i
                            }
                        }
                        i++
                    }
                    insert(length, '}')
                    insert(startIndex, "{$item")
                    return startIndex + item.length + 1
                }
            }

            fun forLoop(key: String) {
                // for(...) or while(...)
                var index = 0
                fun nextIndex() = indexOf(key, index)
                index = nextIndex()
                while (index != -1) {
                    for (i in index + key.length until length) {
                        if (this[i] != ' ') {
                            if (this[i] == '(') {
                                val close = findClose(i, '(', ')', this.toString())
                                if (close != -1) {
                                    var done = false
                                    for (j in close + 1 until length) {
                                        if (this[j] != ' ') {
                                            done = true
                                            index = stepTwo(j)
                                            break
                                        }
                                    }
                                    if (!done) {
                                        index = stepTwo(length)
                                    }
                                    break
                                } else {
                                    return
                                }
                            } else {
                                index++
                                break
                            }
                        }
                    }
                    index = nextIndex()
                }
            }

            fun forFunction() {
                // function f(...){...}
                var index = 0
                fun nextIndex() = indexOf("function", index)
                index = nextIndex()
                while (index != -1) {
                    val close = findClose(indexOf('(', index), '(', ')', this.toString())
                    for (i in close + 1 until length) {
                        if (this[i] == '{') {
                            index = stepTwo(i) + 1
                            break
                        }
                    }
                    index = nextIndex()
                }
            }

            fun forLambda() {
                // let f = (...)=>...
                var index = 0
                fun nextIndex() = indexOf("=>", index)
                index = nextIndex()
                while (index != -1) {
                    for (i in index + 2 until length) {
                        if (this[i] != ' '){
                            index = stepTwo(i) + 1
                            break
                        }
                    }
                    index = nextIndex()
                }
            }
            forLoop("for")
            forLoop("while")
            forFunction()
            forLambda()
        }
    }

    private fun printException(src: String, e: Exception, out: OutputStream = System.out) {
        fun append(src: String) = out.write((if (out is PlayerStream) src.toErrorMessage() else src).toByteArray())
        val exception = if (e is PolyglotException) e else if (e.cause is PolyglotException) e.cause else e
        try {
            if (exception is PolyglotException) {
                if (exception.sourceLocation != null && exception.isGuestException) {
                    append(exception.message + ' ')
                    fun printWithSource(source: SourceSection, showCode: Boolean = false) {
                        append(
                            "at line ${source.startLine}" +
                                    if (source.startLine != source.endLine) " to ${source.endLine}" else "" +
                                            ", column ${source.startColumn}${if (source.startColumn != source.endColumn) " to ${source.endColumn}" else ""}" +
                                            if (showCode)
                                                System.lineSeparator() +
                                                        "   ${
                                                        src.substring(
                                                            startIndex = (source.startColumn - 1 - 10).let { if (it < 0) 0 else it },
                                                            endIndex = (source.endColumn).let { if (it > src.length) src.length else it }
                                                        )
                                                        }<--[HERE]"
                                            else ""
                        )
                    }
                    printWithSource(exception.sourceLocation, true)

                    val count = exception.polyglotStackTrace.count()
                    for (i in 1 until count.let { if (it > 10) 10 else it }) {
                        val it = exception.polyglotStackTrace.elementAt(i)
                        if (it.sourceLocation != null) {
                            printWithSource(it.sourceLocation)
                        }
                    }
                    if (count > 10) {
                        append("${count - 9} more.")
                    }
                } else {
                    val sw = StringWriter()
                    if (exception.isHostException)
                        with(exception.asHostException()) {
                            append("${this::class.simpleName}: ${message}")
                        }
                    else
                        e.printStackTrace(PrintWriter(sw))
                    out.write(sw.toString().toByteArray())
                }
            } else {
                append("${e::class.simpleName}: ${e.message}")
            }
        } catch (e: Exception){
            append("Failed to print exception. Rolling back.")
            e.printStackTrace(PrintWriter(out))
        }
        out.flush()
    }
}