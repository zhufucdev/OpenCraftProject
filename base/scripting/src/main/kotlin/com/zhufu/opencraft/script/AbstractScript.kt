package com.zhufu.opencraft.script

import com.zhufu.opencraft.*
import com.zhufu.opencraft.player_community.PlayerOutputStream
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.SourceSection
import org.graalvm.polyglot.Value
import java.io.File
import java.io.OutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService

abstract class AbstractScript : Callable<Value?>, Nameable {
    abstract val executor: Scripting.Executor
    override var name: String = getLang(
        Language.LANG_ZH, "scripting.unnamed"
    )
    protected abstract var context: Context
    protected abstract val language: String
    var srcFile: File? = null
    var src = ""
    val schedule = ExecutionSchedule()

    protected fun beforeRun(out: OutputStream? = null) {
        val output = out ?: System.out
        val getter = if (out is PlayerOutputStream) out.lang else Language.LangGetter(Language.defaultLangCode)
        with(output) {
            fun write(src: String) = write(src.toByteArray())
            write(getter["scripting.toRun.1"].toInfoMessage() + '\n')
            write(getter["scripting.toRun.2", Scripting.version, executor.name, name].toInfoMessage() + '\n')
            write(getter["scripting.toRun.3"] + '\n')
            flush()
        }
    }

    override fun call(): Value? {
        return null
    }

    protected fun printException(src: String, e: Exception, out: OutputStream = System.out) {
        e.printStackTrace()
        fun append(src: String) = out.write((if (out is PlayerOutputStream) src.toErrorMessage() else src).toByteArray())
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
                            append("${this::class.simpleName}: $message")
                        }
                    else
                        e.printStackTrace(PrintWriter(sw))
                    out.write(sw.toString().toByteArray())
                }
            } else {
                append("${e::class.simpleName}: ${e.message}")
            }
        } catch (e: Exception) {
            append("Failed to print exception. Rolling back.")
            e.printStackTrace(PrintWriter(out))
        }
        out.flush()
    }

    override fun equals(other: Any?): Boolean = other is AbstractScript && other.name == name && other.src == src
    override fun hashCode(): Int {
        var result = executor.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + (srcFile?.hashCode() ?: 0)
        result = 31 * result + src.hashCode()
        return result
    }

    companion object {
        lateinit var threadPool: ExecutorService
    }
}