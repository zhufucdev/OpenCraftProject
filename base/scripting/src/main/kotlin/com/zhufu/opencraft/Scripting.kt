package com.zhufu.opencraft

import com.zhufu.opencraft.events.SSLoadCompleteEvent
import com.zhufu.opencraft.events.SSReloadEvent
import groovy.lang.Binding
import groovy.lang.GroovyShell
import groovy.util.GroovyScriptEngine
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.OutputStream
import java.nio.file.Paths

object Scripting {
    private lateinit var engine: GroovyScriptEngine
    private lateinit var binding: Binding
    internal lateinit var plugin: JavaPlugin

    var status: Status = Status.PRELOAD
        private set

    /**
     * Loads every single Groovy file under ./plugins and ./plugins/ss_modules
     * @return a list of files failed to be loaded
     */
    fun init(plugin: JavaPlugin): List<File> {
        status = Status.LOADING

        this.plugin = plugin
        binding = Binding()
        engine = GroovyScriptEngine(
            arrayOf(
                Paths.get("plugins").toUri().toURL(),
                Paths.get("plugins/ss_modules").toUri().toURL()
            )
        )
        val failures = arrayListOf<File>()
        val load: (File) -> Unit = {
            try {
                engine.run(it.name, binding)
            } catch (e: Exception) {
                e.printStackTrace()
                failures.add(it)
            }
        }
        val filter: (File) -> Boolean = { f -> f.isFile && !f.isHidden && f.extension == "groovy" }
        File("plugins").listFiles(filter)!!.forEach(load)

        Bukkit.getPluginManager().callEvent(SSLoadCompleteEvent(failures))
        status = Status.LOADED
        return failures
    }

    fun cleanUp() {
        if (!::engine.isInitialized) return
        Bukkit.getPluginManager().callEvent(SSReloadEvent())
    }

    @Synchronized
    fun execute(script: String, output: OutputStream? = null): Any? {
        if (status != Status.LOADED) error("Script Engine not ready.")
        Bukkit.getLogger().info("Anonymous script execution: $script")
        val defaultOutput = System.out
        if (output != null) {
            binding.invokeMethod("System.setOut", output)
        }
        val result = GroovyShell(engine.groovyClassLoader, binding).evaluate(script, "console")
        if (output != null) {
            binding.invokeMethod("System.setOut", defaultOutput)
        }
        return result
    }
}