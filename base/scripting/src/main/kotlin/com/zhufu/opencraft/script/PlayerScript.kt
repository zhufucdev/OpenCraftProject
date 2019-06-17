package com.zhufu.opencraft.script

import com.google.common.cache.CacheBuilder
import com.zhufu.opencraft.*
import com.zhufu.opencraft.Scripting.Executor
import com.zhufu.opencraft.headers.PlayerHeaders
import com.zhufu.opencraft.headers.PublicHeaders
import com.zhufu.opencraft.headers.ServerHeaders
import org.bukkit.Bukkit
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import java.io.File
import java.io.OutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import kotlin.collections.ArrayList

class PlayerScript : AbstractScript {
    companion object {
        private val cache = CacheBuilder.newBuilder().maximumSize(20).build<UUID, ArrayList<PlayerScript>>()
        fun list(player: ServerPlayer): ArrayList<PlayerScript> =
            cache[player.uuid ?: throw IllegalArgumentException("Parameter [player.uuid] must not be null."), {
                val r = arrayListOf<PlayerScript>()
                player.scriptDir.listFiles().forEach {
                    r.add(
                        PlayerScript(
                            player = player,
                            srcFile = it,
                            name = it.nameWithoutExtension
                        )
                    )
                }
                r
            }]
    }

    val player: ServerPlayer
    val logger: ServerLogger
    val out: OutputStream
    override val language: String
        get() = player.userLanguage
    override var context: Context
    private var mName = ""
    override var name: String
        get() = mName
        set(value) {
            mName = value
            if (srcFile?.exists() == true){
                srcFile!!.renameTo(File(srcFile!!.parentFile, "$mName.js"))
            }
        }

    private constructor(player: ServerPlayer) : super() {
        this.player = player
        name = getLang(player, "scripting.unnamed")
        logger = player.newLogger(Level.SEVERE)
        out = if (player is ChatInfo) (player as ChatInfo).playerStream else logger.stream()
        context = buildNewContext()
    }

    constructor(player: ServerPlayer, srcFile: File? = null, name: String) : this(player) {
        this.name = name
        this.srcFile = srcFile
        src = srcFile?.readText() ?: ""
    }

    constructor(player: ServerPlayer, src: String) : this(player) {
        this.src = src
    }

    private fun buildNewContext() = Context.newBuilder().out(out).allowHostAccess(true).build()

    override val executor: Executor
        get() = if (player.isOp) Scripting.Executor.Operator else Executor.Player
    override fun call(): Value? {
        beforeRun(out)
        with(context.getBindings("js")) {
            PublicHeaders(language,this@PlayerScript,player).members.forEach {
                putMember(it.first, it.second)
            }
        }
        with(context.getBindings("js")) {
            PlayerHeaders(player, executor).members.forEach {
                putMember(it.first, it.second)
            }
            if (executor == Scripting.Executor.Operator) {
                ServerHeaders.members.forEach {
                    putMember(it.first, it.second)
                }
            }
        }

        val timeBegin = System.currentTimeMillis()
        val result = try {
            checkRunnability()
            threadPool.submit<Value> {
                val r = context.eval(
                    "js",
                    rewriteSrc().also { Bukkit.getLogger().info("${player.name}'s actual execution: $it") })
                schedule.runAll()
                r
            }.get()//Scripting.timeOut, TimeUnit.MILLISECONDS]
        } catch (e: Exception) {
            printException(src, e, out)
            null
        }
        val timeEnd = System.currentTimeMillis()

        val getter = Language.LangGetter(player)
        if (result == null) {
            out.write(
                getter["scripting.returnNull",
                        (timeEnd - timeBegin) / 1000.0].toErrorMessage().toByteArray()
            )
        } else {
            out.write(
                getter["scripting.returnSomething",
                        (timeEnd - timeBegin) / 1000.0, result.toString()].toSuccessMessage().toByteArray()
            )
        }
        out.flush()
        if (player !is ChatInfo)
            logger.save(name)

        Scripting.loopExecutions.remove(player.uuid)
        return result
    }

    var isClosed = false
        private set

    fun close() {
        context.close()
        isClosed = true
    }

    fun reset() {
        close()
        context = buildNewContext()
        isClosed = false
    }

    fun write() {
        File(player.scriptDir, "$name.js").also { if (!it.exists()) it.createNewFile() }.writeText(src)
    }

    private fun checkRunnability() {
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

    private fun rewriteSrc(): String {
        val item = "util.loopBump('${player.uuid.toString()}');"
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
                for (j in startIndex - 1..0) {
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
                        when (this[i]) {
                            '(' -> {
                                // To skip it
                                i = findClose(i, '(', ')', this.toString())
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
                                val open = findOpen(i, '(', ')', this.toString())
                                if (open <= startIndex) {
                                    insert(i, '}')
                                    insert(startIndex, "{$item")
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
                        if (this[i] != ' ') {
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
}