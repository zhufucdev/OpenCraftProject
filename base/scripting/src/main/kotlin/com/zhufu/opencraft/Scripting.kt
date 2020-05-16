package com.zhufu.opencraft

import org.bukkit.plugin.java.JavaPlugin
import org.graalvm.polyglot.Context
import java.io.File

object Scripting {
    const val BUKKIT_PREFIX = "bukkit:"
    const val SS_PREFIX = "ss:"
    val modulesDir get() = File("plugins/ss_modules")

    private val loaders = ArrayList<ModuleLoader>()
    fun load(file: File, requester: Module? = null): ModuleLoader =
        loaders.firstOrNull { it.file == file && it.shareContext == true }
            ?.apply { if (requester != null) this.requester = requester }
            ?: ModuleLoader(file, requester).also { if (it.shareContext == true) loaders.add(it) }

    fun indexAbsolutelyOf(path: String): ModuleLoader? = loaders.firstOrNull { it.path == path }
    fun indexOfName(name: String): ModuleLoader? =
        loaders.firstOrNull { it.name == name && File("plugins", it.path).parentFile.parentFile == modulesDir }

    fun indexOfContext(context: Context): Module? =
        loaders.firstOrNull { it.isLoaded && it.load()!!.let { module -> module.context == context } }?.load()

    fun indexFriendly(name: String, relativeTo: File, requester: Module? = null): ModuleLoader {
        var result: ModuleLoader? = null
        if (name.contains('/')) {
            result = indexAbsolutelyOf(name)
        }
        if (result == null) {
            result = loaders.firstOrNull { m ->
                m.name == name && File(
                    "plugins",
                    m.path
                ).let { it != relativeTo && it.parentFile == relativeTo.parentFile }
            }
                ?: load(File(relativeTo.parentFile, "$name.js"), requester)
        }
        if (!result.canLoad) {
            val test = if (name.contains('/')) File(modulesDir, "$name.js") else File(modulesDir, "$name/main.js")
            if (test.exists())
                result = indexAbsolutelyOf(test.relativeTo(File("plugins")).path)
                    ?: load(test, requester)
        }
        return result
    }

    internal val lockers = HashMap<Context, Thread>()

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
     * Loads every single JavaScript file under ./plugins and ./plugins/ss_module
     * @return a list of files failed to be loaded
     */
    fun init(plugin: JavaPlugin): List<File> {
        this.plugin = plugin
        val r = arrayListOf<File>()
        val load: (File) -> Unit = {
            load(it).let { m ->
                if (m.load() == null || m.initializationException != null) r.add(it)
            }
        }
        File("plugins").listFiles { f: File -> f.isFile && !f.isHidden && f.extension == "js" }!!
            .forEach(load)
        File("ss_modules").listFiles { f -> f.isFile && !f.isHidden && f.extension == "js" }!!
            .forEach(load)
        return r
    }

    fun cleanUp() {
        loaders.forEach { if (it.isLoaded) it.load()!!.disable() }
        loaders.clear()
    }
}