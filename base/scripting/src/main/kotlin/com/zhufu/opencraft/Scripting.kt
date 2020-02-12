package com.zhufu.opencraft

import org.bukkit.plugin.java.JavaPlugin
import org.graalvm.polyglot.Context
import java.io.File

object Scripting {
    val loaded = ArrayList<Module>()
    fun load(file: File, requester: Module? = null): Module? {
        return if (file.exists()) {
            try {
                val m = Module(file, requester)
                loaded.add(m)
                m.init()
                m
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else null
    }

    fun indexAbsolutelyOf(path: String): Module? = loaded.firstOrNull { it.path == path }
    fun indexOfName(name: String): Module? =
        loaded.firstOrNull { it.name == name && File("plugins", it.path).parentFile.parentFile == modulesDir }
    fun indexOfContext(context: Context): Module? =
        loaded.firstOrNull { it.context == context }

    private val lockers = HashMap<Context, Thread>()
    @Suppress("ControlFlowWithEmptyBody")
    fun <R> syncCall(context: Context, block: () -> R): R {
        fun wait() {
            val currentThread = Thread.currentThread()
            while (lockers.containsKey(context)) {
            }
            lockers[context] = currentThread
            if (lockers[context] != currentThread) wait()
        }
        synchronized(context) {
            wait()
        }
        try {
            val r = block()
            lockers.remove(context)
            return r
        } catch (e: Exception) {
            lockers.remove(context)
            throw e
        }
    }

    internal lateinit var plugin: JavaPlugin
    /**
     * @return a list of files failed to be loaded
     */
    fun init(plugin: JavaPlugin): List<File> {
        this.plugin = plugin
        val r = arrayListOf<File>()
        File("plugins").listFiles { f: File -> f.extension == "js" }!!
            .forEach { file -> load(file).let { m -> if (m == null) r.add(file) } }
        return r
    }

    fun cleanUp() {
        loaded.forEach { it.disable() }
        loaded.clear()
    }

    val modulesDir get() = File("plugins/ss_modules")
}