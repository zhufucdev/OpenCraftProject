package com.zhufu.opencraft

import com.zhufu.opencraft.events.SSReloadEvent
import groovy.lang.Binding
import groovy.util.GroovyScriptEngine
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.lang.reflect.Modifier
import java.nio.file.Paths

object Scripting {
    private lateinit var engine: GroovyScriptEngine
    private lateinit var binding: Binding
    internal lateinit var plugin: JavaPlugin

    /**
     * Loads every single Groovy file under ./plugins and ./plugins/ss_modules
     * @return a list of files failed to be loaded
     */
    fun init(plugin: JavaPlugin): List<File> {
        this.plugin = plugin
        binding = Binding()
        engine = GroovyScriptEngine(
            arrayOf(
                Paths.get("plugins").toUri().toURL(),
                Paths.get("plugins/ss_modules").toUri().toURL()
            )
        )
        val r = arrayListOf<File>()
        val load: (File) -> Unit = {
            engine.run(it.name, binding)
        }
        val filter: (File) -> Boolean = { f -> f.isFile && !f.isHidden && f.extension == "groovy" }
        File("plugins").listFiles(filter)!!.forEach(load)
        return r
    }

    fun cleanUp() {
        if (!::engine.isInitialized) return
        Bukkit.getPluginManager().callEvent(SSReloadEvent())
    }
}