package com.zhufu.opencraft.script

import com.zhufu.opencraft.Language
import com.zhufu.opencraft.Scripting
import com.zhufu.opencraft.ServerPlayer
import com.zhufu.opencraft.headers.PublicHeaders
import com.zhufu.opencraft.headers.ServerHeaders
import com.zhufu.opencraft.headers.server_wrap.ServerSelf
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import java.io.File
import java.io.OutputStream
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class ServerScript private constructor() : AbstractScript() {
    override val executor: Scripting.Executor
        get() = Scripting.Executor.Sever
    override val language: String
        get() = Language.defaultLangCode
    var outputStream: OutputStream? = null
    private val out = object : OutputStream() {
        override fun write(b: Int) {
            (outputStream ?: System.out).write(b)
        }

        override fun write(b: ByteArray?) {
            (outputStream ?: System.out).write(b)
        }

        override fun flush() {
            (outputStream ?: System.out).flush()
        }
    }
    private fun buildContext() = Context.newBuilder().allowAllAccess(true).out(out).build()!!
    override var context = buildContext()

    init {
        name = "AutoExec"
        srcFile = Paths.get("plugins", "ServerCore", "script.js").toFile().also {
            if (!it.exists())
                it.createNewFile()
        }
        src = srcFile!!.readText()
    }

    /**
     * This [call] is special as an initialization. So it should only be called once.
     */
    override fun call(): Value? {
        with(context.getBindings("js")) {
            PublicHeaders(language).members.forEach {
                putMember(it.first, it.second)
            }
        }
        beforeRun(out)
        with(context.getBindings("js")){
            ServerHeaders.members.forEach {
                putMember(it.first, it.second)
            }
        }
        return context.eval("js", src)
    }

    fun runLine(src: String, out: OutputStream? = null): Value? = try {
        beforeRun(out)
        outputStream = out
        threadPool.submit<Value> {
            context.eval("js", src)
        }[Scripting.timeOut, TimeUnit.MILLISECONDS]
    } catch (e: Exception) {
        printException(src, e, out ?: System.out)
        null
    }

    companion object {
        val INSTANCE = ServerScript()
        fun reload(){
            INSTANCE.apply {
                context = buildContext()
                src = srcFile!!.readText()
            }
            ServerHeaders.serverSelf = ServerSelf()
        }
    }
}