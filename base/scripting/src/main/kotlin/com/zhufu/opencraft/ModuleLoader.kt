package com.zhufu.opencraft

import java.io.File
import java.util.concurrent.FutureTask

class ModuleLoader(val file: File, var requester: Module? = null) {
    val path: String by lazy { file.relativeTo(File("plugins")).path }
    val name =
        if (file.extension != "js")
            throw IllegalArgumentException("[file] must be named as .js")
        else if (file.parentFile.parentFile == Scripting.modulesDir && file.name == "main.js")
            file.parentFile.nameWithoutExtension
        else
            file.nameWithoutExtension
    val isDependency by lazy { file.parentFile != File("plugins") && file.parentFile.parentFile == Scripting.modulesDir }

    private lateinit var module: Module
    val isLoaded: Boolean get() = ::module.isInitialized
    val canLoad: Boolean get() = file.exists() && file.canRead() && file.extension == "js"
    private var isLoading = false
    var initializationException: Exception? = null
    val shareContext by lazy {
        // Try to load the module if it's not been loaded
        if (!isLoaded) {
            if (!isLoading)
                load()
            else
                FutureTask { while (!isLoaded){} }.get()
        }
        if (isLoaded)
            module.shareContext
        else
            null
    }
    fun load(): Module? =
        if (!isLoaded) {
            if (canLoad) {
                if (file.exists()) {
                    try {
                        isLoading = true
                        module = Module(this, requester)
                        module.init()
                        postLoadListener.forEach { it.invoke() }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        initializationException = e
                    } finally {
                        isLoading = false
                    }
                    module
                } else null
            } else null
        } else {
            module
        }

    private var postLoadListener = ArrayList<(() -> Unit)>()
    fun addPostLoadListener(l: () -> Unit) {
        postLoadListener.add(l)
    }

    override fun equals(other: Any?): Boolean = other is ModuleLoader && other.path == path
    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}