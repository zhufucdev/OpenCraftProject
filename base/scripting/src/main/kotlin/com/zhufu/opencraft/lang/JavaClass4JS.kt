package com.zhufu.opencraft.lang

import com.zhufu.opencraft.Module
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyObject
import java.lang.reflect.Constructor
import kotlin.reflect.*
import kotlin.reflect.full.*

class JavaClass4JS : ProxyObject, ProxyInstantiable, ProxyWrap {
    internal val clazz: Class<*>
    private val wrapper: Context
    private var instance: Any? = null
    /* Static Fields */
    private val extends = hashMapOf<String, Value?>()
    private val functions = hashMapOf<String, ProxyExecutable>()
    private val getters = hashMapOf<String, () -> Any?>()

    constructor(clazz: Class<*>, wrapper: Context) {
        this.clazz = clazz
        this.wrapper = wrapper

        if (clazz.kotlin.objectInstance != null) {
            instance = clazz.kotlin.objectInstance
            init(false)
        } else {
            init(true)
        }
    }

    constructor(obj: Any, wrapper: Context) {
        this.clazz = obj::class.java
        this.wrapper = wrapper
        this.instance = obj

        init(false)
    }

    fun init(static: Boolean) {
        val kotlin = clazz.kotlin
        if (clazz.isEnum) {
            clazz.fields.forEach {
                getters[it.name] = { it.get(null) }
            }
        }

        val f = hashMapOf<String, ArrayList<KFunction<*>>>()
        val eachFun: (KFunction<*>) -> Unit = {
            if (it.visibility == KVisibility.PUBLIC) {
                if (!f.containsKey(it.name)) {
                    f[it.name] = arrayListOf()
                }
                f[it.name]!!.add(it)
            }
        }
        val eachProperty: (KProperty<*>) -> Unit = {
            if (it.visibility == KVisibility.PUBLIC) {
                getters[it.name] = {
                    if (static)
                        it.call()
                    else
                        it.call(instance)
                }
            }
        }
        if (static) {
            kotlin.staticFunctions.forEach(eachFun)
            kotlin.staticProperties.forEach(eachProperty)
        } else {
            kotlin.memberFunctions.forEach(eachFun)
            kotlin.memberExtensionFunctions.forEach(eachFun)
            kotlin.memberProperties.forEach(eachProperty)
            kotlin.memberExtensionProperties.forEach(eachProperty)
        }
        kotlin.companionObjectInstance?.let { companion ->
            companion::class.functions.forEach(eachFun)
            companion::class.memberExtensionFunctions.forEach(eachFun)
            companion::class.memberProperties.forEach(eachProperty)
            companion::class.memberExtensionProperties.forEach(eachProperty)
        }
        f.forEach { (k, v) ->
            functions[k] = ProxyExecutable { arguments ->
                val args = Module.javalize(arguments, wrapper, false)
                fun typeMatch(a: KParameter, b: Any?) =
                    b == null || a.type.isSupertypeOf(b::class.defaultType)

                val filter: (KFunction<*>) -> Boolean = if (static) {
                    {
                        if (it.parameters.size == args.size) {
                            var result = true
                            for (i in args.indices) {
                                if (!typeMatch(it.parameters[i], args[i])) {
                                    result = false
                                    break
                                }
                            }
                            result
                        } else {
                            false
                        }
                    }
                } else {
                    {
                        if (it.parameters.size - 1 == args.size) {
                            var result = true
                            for (i in args.indices) {
                                if (!typeMatch(it.parameters[i + 1], args[i])) {
                                    result = false
                                    break
                                }
                            }
                            result
                        } else {
                            false
                        }
                    }
                }
                (v.firstOrNull(filter)
                    ?: throw RuntimeException(
                        "No such function: $k(${
                        buildString {
                            args.forEach {
                                append(if (it == null) "Any" else it::class.simpleName)
                                append(", ")
                            }
                        }.removeSuffix(", ")
                        })."
                    )).let {
                    if (static)
                        it.call(*args)
                    else
                        it.call(*arrayListOf(instance).apply { addAll(args) }.toArray())
                }

            }
        }
    }

    override fun putMember(key: String, value: Value?) {
        if (functions.containsKey(key) || getters.containsKey(key) || clazz.classes.any { it.simpleName == key })
            throw RuntimeException("$key of ${clazz.packageName} is already declared.")
        extends[key] = value
    }

    override fun getMemberKeys(): Any? =
        ProxyArray.fromList(
            listOf<String>()
                .asSequence()
                .plus(extends.keys)
                .plus(functions.keys)
                .plus(getters.keys)
                .plus(clazz.classes.map { it.simpleName }).plus("javaClass")
                .toList()
        )

    override fun getMember(key: String): Any? = when {
        key == "javaClass" -> clazz
        getters.containsKey(key) -> getters[key]!!.invoke()
        functions.containsKey(key) -> functions[key]
        extends.containsKey(key) -> extends[key]
        clazz.classes.any { it.simpleName == key } -> JavaClass4JS(
            clazz.classes.first { it.simpleName == key },
            wrapper
        )
        else -> throw RuntimeException("$key in ${if (extends.isEmpty()) "" else "extended "}class ${clazz.packageName} doesn't exist.")
    }

    override fun hasMember(key: String?): Boolean =
        key == "javaClass" || getters.containsKey(key) || functions.containsKey(key)
                || extends.containsKey(key) || clazz.classes.any { it.simpleName == key }

    override fun newInstance(vararg arguments: Value?): Any {
        val argsJavalized =
            Module.javalize(arrayOf(*arguments), wrapper)
        val argsTyped = argsJavalized.map { it?.let { it::class.java } ?: Any::class.java }
        val constructor: Constructor<*> = clazz.declaredConstructors.firstOrNull {
            if (it.parameterCount == argsTyped.size) {
                var result = true
                for (i in 0 until it.parameterCount - 1) {
                    if (!it.parameterTypes[i].kotlin.isSuperclassOf(argsTyped[i].kotlin)) {
                        result = false
                        break
                    }
                }
                result
            } else {
                false
            }
        }
            ?: throw RuntimeException("Constructor for ${clazz.packageName}(${buildString {
                argsTyped.forEach {
                    append(it.simpleName)
                    append(", ")
                }
            }.removeSuffix(", ")}) doesn't exit.")
        return JavaClass4JS(constructor.newInstance(*argsJavalized), wrapper)
    }

    override fun rewrap(newWrapper: Context): ProxyWrap =
        if (instance == null) JavaClass4JS(clazz, newWrapper) else JavaClass4JS(instance!!, newWrapper)
}