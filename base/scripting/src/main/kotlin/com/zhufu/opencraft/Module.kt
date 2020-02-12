package com.zhufu.opencraft

import com.zhufu.opencraft.lang.JSContainer
import com.zhufu.opencraft.lang.JavaClass4JS
import com.zhufu.opencraft.lang.ProxyWrap
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.*
import java.io.File
import java.io.Reader
import java.io.Writer
import java.util.function.Function
import kotlin.concurrent.thread

class Module(val file: File, internal var requester: Module? = null) {
    val path: String = file.relativeTo(File("plugins")).path
    val name =
        if (file.extension != "js")
            throw IllegalArgumentException("[file] must be named as .js")
        else if (file.parentFile.parentFile == Scripting.modulesDir && file.name == "main.js")
            file.parentFile.nameWithoutExtension
        else
            file.nameWithoutExtension
    val isDependency = file.parentFile != File("plugins") && file.parentFile.parentFile != Scripting.modulesDir

    private val requireFunc: Function<String, Any?> = Function { r ->
        var result: Module? = null
        if (r.contains('/')) {
            result = Scripting.indexAbsolutelyOf(r)
        }
        if (result == null) {
            result = Scripting.loaded.firstOrNull { m ->
                m.name == r && File("plugins", m.path).parentFile == file.parentFile
            }
                ?: Scripting.load(File(file.parentFile, "$r.js"), this)
        }
        if (result == null) {
            val test = if (r.contains('/')) File("plugins", r) else File(Scripting.modulesDir, "$r/main.js")
            if (test.exists())
                result = Scripting.indexAbsolutelyOf(test.relativeTo(File("plugins")).path)
                    ?: Scripting.load(test, this)
        }
        return@Function result?.let {
            val v = result.module.getMember("exports")
            if (v != null)
                javalize(v, context)
            else
                null
        }
    }
    val context =
        Context.newBuilder("js")
            .allowAllAccess(true)
            .build()
    val binding = context.getBindings("js")
    val module: Value
        get() = binding.getMember("module")

    init {
        Bukkit.getLogger().info("Context for module $name is ${this.context}.")
        binding.putMember(
            "module", ProxyObject.fromMap(
                mapOf(
                    "path" to path,
                    "name" to name,
                    "isDependency" to isDependency
                )
            )
        )
        binding.putMember("require", requireFunc)
    }

    internal fun init() {
        val source = Source
            .newBuilder("js", file.reader(), name)
            .build()

        file.bufferedReader().readText()
        source.getCharacters(1).let { l1 ->
            if (l1.startsWith("'use ")) {
                val start = l1.indexOf(' ') + 1
                val end = l1.indexOf("'", start)
                l1.substring(start, end).split(',').forEach {
                    val usage = it.trim()
                    val member = extraMember(usage)
                    if (member != null)
                        binding.putMember(member.first, member.second)
                    else
                        Bukkit.getLogger().warning("Extra member for usage $usage doesn't exist. Ignoring.")
                }
            }
        }
        Scripting.syncCall(context) {
            context.eval(source)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun disable() {
        val close = module.getMember("onDisable");
        if (close != null) {
            Scripting.syncCall(context) {
                try {
                    close.execute()
                } catch (e: Exception) {
                    Bukkit.getLogger()
                        .warning("Error while disabling server script $name (${e::class.simpleName}): ${e.message}")
                }
                context.close(true)
            }
        }
    }

    private fun extraMember(usage: String): Pair<String, Any?>? = when (usage) {
        "thread" -> usage to ProxyExecutable { args ->
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
        "readerToString" -> usage to ProxyExecutable {
            val reader = it.first().asHostObject<Reader>()
            reader.readText()
        }
        "copyReader" -> usage to ProxyExecutable {
            val reader = it.first().asHostObject<Reader>()
            val writer = it[1].asHostObject<Writer>()
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
        "requester" -> usage to ProxyExecutable {
            requester?.let { javalize(it.module, context) }
        }
        "newListener" -> usage to ProxyExecutable {
            return@ProxyExecutable object : Listener {}
        }
        "javaPlugin" -> usage to Scripting.plugin
        "listJava" -> usage to ProxyExecutable { args ->
            val namePrefix = args.first().asString()
            ProxyArray.fromList(Package.getPackages().filter { it.name.startsWith(namePrefix) }.map { it.name })
        }
        "requireJava" -> usage to ProxyExecutable {
            val name = it.first().asString()
            val clazz = try {
                Class.forName(name)
            } catch (e: ClassNotFoundException) {
                throw RuntimeException("No such class: $name.")
            }
            JavaClass4JS(clazz, context)
        }
        "getJavaClass" -> usage to ProxyExecutable {
            if (it.first().isProxyObject) {
                try {
                    val cast = it.first().asProxyObject<JavaClass4JS>()
                    return@ProxyExecutable cast.clazz
                } catch (ignore: Exception) {
                }
            }
            val obj = it.first().`as`(Any::class.java)
            obj.javaClass
        }
        /*
        object({
            p1: {
                get: () => //Does something,
            },
            p2: {
                get: () => //Does something
                set: (value) => //Does something
            }
        })
        This will get such object with getters and setters.
         */
        "object" -> usage to ProxyExecutable {
            val selections = it.first()
            if (!selections.hasMembers())
                throw RuntimeException("Parameter doesn't have members.")
            JSContainer(selections, context)
        }
        "createEventExecutor" -> usage to ProxyExecutable {
            val f = it.first()
            EventExecutor { listener, event ->
                f.execute(listener, event)
            }
        }
        else -> null
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun javalize(jsObject: Value, wrapper: Context): Any? {
            return when {
                jsObject.isNull -> null
                jsObject.isProxyObject -> {
                    try {
                        val cast = jsObject.asProxyObject<ProxyWrap>()
                        cast.rewrap(wrapper)
                    } catch (ignore: Exception) {
                        jsObject
                    }
                }
                jsObject.isHostObject -> jsObject.asHostObject()
                jsObject.canExecute() -> JSContainer(jsObject, wrapper)
                jsObject.isString -> jsObject.asString()
                jsObject.isNumber -> jsObject.`as`(Number::class.java)
                jsObject.isBoolean -> jsObject.asBoolean()
                jsObject.isDate -> ProxyDate.from(jsObject.asDate())
                jsObject.hasArrayElements() -> {
                    val array = arrayListOf<Any?>()
                    for (i in 0 until jsObject.arraySize) {
                        array.add(javalize(jsObject.getArrayElement(i), wrapper))
                    }
                    ProxyArray.fromList(array)
                }
                jsObject.hasMembers() -> {
                    if (jsObject.memberKeys.contains("prototype")) {
                        val prototype = jsObject.getMember("prototype")
                        if (prototype.isProxyObject) {
                            try {
                                val cast = prototype.asProxyObject<JSContainer>()
                                val map = hashMapOf<String, JSContainer.Member>()

                                map.putAll(cast.map)
                                jsObject.memberKeys.forEach {
                                    if (it != "prototype") {
                                        map[it] = JSContainer.Direct(jsObject.getMember(it), wrapper)
                                    }
                                }

                                return JSContainer(map, wrapper)
                            } catch (ignore: Exception) {
                            }
                        }
                    }
                    val map = LinkedHashMap<String, Any?>()
                    jsObject.memberKeys.forEach {
                        if (it == "prototype") {
                            val prototype = jsObject.getMember(it)
                            prototype.memberKeys.forEach { prototypeMember ->
                                map[prototypeMember] = javalize(prototype.getMember(prototypeMember), wrapper)
                            }
                        } else {
                            map[it] = javalize(jsObject.getMember(it), wrapper)
                        }
                    }
                    ProxyObject.fromMap(map)
                }
                else -> null
            }
        }

        fun javalize(objects: Array<Value?>, wrapper: Context): Array<Any?> {
            val r = arrayListOf<Any?>()
            objects.forEach { r.add(if (it != null) javalize(it, wrapper) else null) }
            return r.toArray()
        }
    }
}