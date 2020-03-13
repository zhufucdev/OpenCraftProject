package com.zhufu.opencraft

import com.zhufu.opencraft.Scripting.BUKKIT_PREFIX
import com.zhufu.opencraft.Scripting.SS_PREFIX
import com.zhufu.opencraft.lang.JSContainer
import com.zhufu.opencraft.lang.JavaClass4JS
import com.zhufu.opencraft.lang.ProxyWrap
import org.bukkit.Bukkit
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.plugin.EventExecutor
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.*
import java.io.Reader
import java.io.Writer
import kotlin.concurrent.thread

class Module(val loader: ModuleLoader, internal var requester: Module? = null) {
    val path: String get() = loader.path
    val name get() = loader.name
    val isDependency get() = loader.isDependency

    private val requireFunc = ProxyExecutable { r ->
        if (r.isEmpty() || !r.first().isString) {
            throw RuntimeException("Arguments must contain a {string}.")
        }
        val index = r.first().asString()
        val loader = Scripting.indexFriendly(index, loader.file, this)
        if (!loader.canLoad)
            throw RuntimeException("No such module: $index")

        return@ProxyExecutable loader.load()?.let {
            if (requester?.context == it.context && !isInitialized)
                throw RuntimeException(
                    "Circle Depend. The requester of this module is ${requester!!.name}, " +
                            "which is required by this module."
                )
            val v = it.module.getMember("exports")
            if (v != null)
                javalize(v, context)
            else
                null
        }
    }
    private val afterFunc = ProxyExecutable {
        if (it.size < 2 || !it.first().isString || !it[1].canExecute()) {
            throw RuntimeException("Arguments must contains a {string} and a {function}.")
        }
        var what = it.first().asString()
        val execute = it[1]
        fun notifyLoad() {
            thread {
                Scripting.syncCall(context) {
                    execute.executeVoid()
                }
            }
        }
        if (what.startsWith(BUKKIT_PREFIX)) {
            what = what.removePrefix(BUKKIT_PREFIX)
            Bukkit.getPluginManager().apply {
                if (getPlugin(what) == null) throw RuntimeException("Bukkit plugin $what doesn't exist.")

                if (isPluginEnabled(what)) {
                    notifyLoad()
                } else {
                    registerEvent(PluginEnableEvent::class.java, object : Listener {}, EventPriority.NORMAL, { l, e ->
                        val event = e as PluginEnableEvent
                        if (event.plugin.name == what) {
                            notifyLoad()
                            HandlerList.unregisterAll(l)
                        }
                    }, Scripting.plugin)
                }
            }
        } else {
            if (what.startsWith(SS_PREFIX)) {
                what = what.removePrefix(SS_PREFIX)
            }
            val loader = Scripting.indexFriendly(what, loader.file)
            if (!loader.canLoad)
                throw RuntimeException("The module $what doesn't exist or cannot be loaded.")

            if (loader.isLoaded) notifyLoad()
            else {
                loader.addPostLoadListener {
                    notifyLoad()
                }
            }
        }
        return@ProxyExecutable null
    }
    internal val context =
        Context.newBuilder("js")
            .allowAllAccess(true)
            .build()
    private val binding = context.getBindings("js")
    val module: Value
        get() = binding.getMember("module")
    private var isInitialized = false

    init {
        if (Game.env.getBoolean("debug"))
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
        binding.putMember("after", afterFunc)
    }

    internal fun init() {
        val source = Source
            .newBuilder("js", loader.file.reader(), name)
            .build()

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
        if (!isDependency)
            Bukkit.getLogger().info("Initialized $name.")
        isInitialized = true
    }

    @Suppress("UNCHECKED_CAST")
    fun disable() {
        if (Scripting.lockers.containsKey(context)) {
            Scripting.lockers[context]!!.interrupt()
        }
        val close = module.getMember("onDisable")
        if (close != null) {
            if (!isDependency)
                Bukkit.getLogger().info("Disabling $name.")
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
        "isModuleLoaded" -> usage to ProxyExecutable {
            var name = it.first().asString()
            if (name.startsWith(BUKKIT_PREFIX)) {
                name = name.removePrefix(BUKKIT_PREFIX)
                return@ProxyExecutable Bukkit.getPluginManager().isPluginEnabled(name)
            } else {
                if (name.startsWith(SS_PREFIX))
                    name = name.removePrefix(SS_PREFIX)
                val loader = Scripting.indexFriendly(name, loader.file)
                return@ProxyExecutable loader.isLoaded
            }
        }
        else -> null
    }

    override fun equals(other: Any?): Boolean = other is Module && other.loader == loader && other.context == context
    override fun hashCode(): Int {
        var result = loader.hashCode()
        result = 31 * result + (requester?.hashCode() ?: 0)
        result = 31 * result + (context?.hashCode() ?: 0)
        return result
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun javalize(jsObject: Value, wrapper: Context, createProxy: Boolean = true): Any? {
            fun asMembers(): ProxyObject {
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
                return ProxyObject.fromMap(map)
            }
            return when {
                jsObject.isNull -> null
                jsObject.isProxyObject -> {
                    try {
                        val cast = jsObject.asProxyObject<ProxyWrap>()
                        cast.rewrap(wrapper)
                    } catch (ignore: Exception) {
                        return if (jsObject.hasMembers()) asMembers() else jsObject
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
                    if (createProxy)
                        ProxyArray.fromList(array)
                    else
                        array.toArray()
                }
                jsObject.hasMembers() -> if (createProxy) asMembers() else UnsupportedOperationException("Object with members cannot be converted into Java object")
                else -> null
            }
        }

        fun javalize(objects: Array<Value?>, wrapper: Context, createProxy: Boolean = true): Array<Any?> {
            val r = arrayListOf<Any?>()
            objects.forEach { r.add(if (it != null) javalize(it, wrapper, createProxy) else null) }
            return r.toArray()
        }
    }
}