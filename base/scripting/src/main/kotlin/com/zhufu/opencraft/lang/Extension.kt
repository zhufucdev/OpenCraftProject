@file:Suppress("unused")

package com.zhufu.opencraft.lang

import com.zhufu.opencraft.Scripting
import org.bukkit.event.Listener
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.io.Reader
import java.io.Writer
import kotlin.concurrent.thread

/**
 * Some context-less Java functions for extension.
 */
object Extension {
    val thread get() = ProxyExecutable { args ->
        val e = args.first()
        fun execute(func: Value) {
            if (func.isProxyObject) {
                try {
                    val cast = func.asProxyObject<JSContainer>()
                    execute(cast.executable!!)
                    return
                } catch (ignored: Exception) {
                }
            }
            Scripting.syncCall(func.context) {
                func.execute()
            }
        }
        if (args.size >= 2) {
            thread(start = false, name = args[1].asString()) { execute(e) }
        } else {
            thread(start = false) { execute(e) }
        }
    }
    fun readerToString(reader: Reader) = reader.readText()
    fun copyReader(reader: Reader, writer: Writer) {
        reader.use {
            var b = reader.read()
            while (b != -1) {
                writer.write(b)
                b = reader.read()
            }
            writer.flush()
            writer.close()
        }
    }
    fun newListener() = object : Listener {}
}