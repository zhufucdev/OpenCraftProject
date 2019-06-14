package com.zhufu.opencraft.script

import com.zhufu.opencraft.Scripting
import com.zhufu.opencraft.ServerPlayer
import com.zhufu.opencraft.headers.ServerHeaders
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import java.io.File
import java.io.OutputStream
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class ServerScript private constructor() : AbstractScript() {
    override val executor: Scripting.Executor
        get() = Scripting.Executor.Sever
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
    override var context = Context.newBuilder().allowAllAccess(true).out(out).build()!!

    init {
        name = "AutoExec"
        srcFile = Paths.get("plugins", "ServerCore", "script.js").toFile().also {
            if (!it.exists())
                it.createNewFile()
        }
    }

    /**
     * This [call] is special as an initialization. So it should only be called once.
     */
    override fun call(): Value? {
        beforeRun(out)
        with(context.getBindings("js")){
            ServerHeaders.members.forEach {
                putMember(it.first, it.second)
            }
        }
        super.call()
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
    }
}