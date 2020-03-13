package com.zhufu.opencraft.lang

import com.zhufu.opencraft.Module
import com.zhufu.opencraft.Scripting
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.*
import java.util.function.Function
import java.util.function.Supplier

class JSContainer : ProxyObject, ProxyExecutable, ProxyInstantiable, ProxyWrap {
    abstract class Member {
        abstract var value: Any?
    }

    class Direct : Member {
        override var value: Any? = null
        internal var from: Value? = null

        constructor(from: Value, wrapper: Context) {
            value = if (from.isHostObject) from.asHostObject() else Module.javalize(from, wrapper)
            this.from = from
        }

        constructor(value: Any?) {
            if (value is Value) throw IllegalArgumentException("Value cannot be from other context.")
            this.value = value
        }
    }

    class Getter(f: Value, wrapper: Context) : Member() {
        internal val f = JSContainer(f, wrapper)
        override var value: Any?
            get() = f.execute()
            set(value) {}
    }

    class GetterSetter(g: Value, s: Value, wrapper: Context) : Member() {
        internal val g = JSContainer(g, wrapper)
        internal val s = JSContainer(s, wrapper)
        override var value: Any?
            get() = g.execute()
            set(value) {
                s.execute(value as Value)
            }
    }

    class Setter(f: Value, wrapper: Context) : Member() {
        internal val f = JSContainer(f, wrapper)
        override var value: Any?
            get() = throw NoSuchMethodException("Getter")
            set(value) {
                f.execute(value as Value)
            }
    }

    internal val map = HashMap<String, Member>()
    private val wrapper: Context
    internal var executable: Value? = null
    private val funcToString = object : ProxyExecutable {
        override fun execute(vararg arguments: Value?): Any {
            return buildString {
                append("{")
                map.forEach { (t, u) ->
                    when (u) {
                        is Direct -> {
                            val v = u.value
                            if (v is JSContainer) {
                                val toString = v.getMember("toString")
                                if (toString is ProxyExecutable)
                                    append("$t: ${try{ toString.execute() } catch (e: Exception) { "Unable to fetch $t: " + e.message } }")
                            } else {
                                append("$t: ${v.toString()}")
                            }
                        }
                        is Getter -> {
                            append("$t: [getter]")
                        }
                        is Setter -> {
                            append("$t: [setter]")
                        }
                        is GetterSetter -> {
                            append("$t: [getter, setter]")
                        }
                    }

                    append(", ")
                }
                delete(lastIndex - 1, length)
                append("}")
            }
        }

        override fun toString(): String = "function { [native code] }"
    }

    constructor(from: Value, wrapper: Context) {
        this.wrapper = wrapper
        if (from.hasMembers()) {
            from.memberKeys.forEach { key ->
                val member = from.getMember(key)
                fun check(name: String) {
                    if (!member.getMember(name).canExecute()) throw RuntimeException("$name of $key must be executable.")
                }
                if (member.hasMember("getter")) {
                    check("getter")
                    if (member.hasMember("setter")) {
                        check("setter")
                        map[key] = GetterSetter(
                            g = member.getMember("getter"),
                            s = member.getMember("setter"),
                            wrapper = wrapper
                        )
                    } else {
                        map[key] = Getter(
                            member.getMember("getter"),
                            wrapper = wrapper
                        )
                    }
                } else {
                    map[key] = Direct(member, wrapper)
                }
            }
        }

        if (from.canExecute()) {
            fun test(what: Value) {
                if (what.isProxyObject) {
                    try {
                        val cast = what.asProxyObject<JSContainer>()
                        if (cast.executable != null) {
                            test(cast.executable!!)
                            return
                        }
                    } catch (ignore: Exception) {
                    }
                }
                this.executable = what
            }
            test(from)
        }

        map["toString"] = Direct(funcToString)
    }

    constructor(from: Map<String, Member>, wrapper: Context) {
        this.wrapper = wrapper
        from.forEach { (t, u) ->
            map[t] = when (u) {
                is Direct -> if (u.from != null) Direct(u.from!!, wrapper) else Direct(u.value)
                is Getter -> Getter(u.f.executable!!, wrapper)
                is GetterSetter -> GetterSetter(u.g.executable!!, u.s.executable!!, wrapper)
                is Setter -> Setter(u.f.executable!!, wrapper)
                else -> throw IllegalArgumentException()
            }
        }
    }

    override fun putMember(key: String, value: Value) {
        if (map.containsKey(key)) {
            map[key]!!.value = value
        } else {
            map[key] = Direct(value, value.context)
        }
    }

    override fun getMemberKeys(): Any = ProxyArray.fromList(map.keys.toList())

    override fun getMember(key: String): Any? {
        if (!map.containsKey(key)) throw RuntimeException("$key is not defined.")
        return map[key]!!.value
    }

    override fun hasMember(key: String?): Boolean = map.containsKey(key)

    override fun rewrap(newWrapper: Context): ProxyWrap =
        JSContainer(map, newWrapper).also { it.executable = executable }

    override fun newInstance(vararg arguments: Value?): Any? {
        if (executable == null || !executable!!.canInstantiate())
            throw RuntimeException("Instantiate on an un-instantiable object.")
        val wrap = executable!!
        Scripting.indexOfContext(wrap.context)?.requester =
            Scripting.indexOfContext(wrapper)
        return if (wrap.context == wrapper)
            wrap.newInstance(*localizeArguments(arguments))
        else Scripting.syncCall(wrap.context) {
            Module.javalize(
                wrap.newInstance(
                    *localizeArguments(
                        arguments
                    )
                ), wrapper
            )
        }
    }

    override fun execute(vararg arguments: Value?): Any? {
        if (executable == null || !executable!!.canExecute())
            throw RuntimeException("Execute an un-executable object.")
        val executable = executable!!

        Scripting.indexOfContext(executable.context)?.requester =
            Scripting.indexOfContext(wrapper)
        return if (executable.context == wrapper)
            executable.execute(*localizeArguments(arguments))
        else Scripting.syncCall(executable.context) {
            Module.javalize(
                executable.execute(*localizeArguments(arguments)),
                wrapper
            )
        }
    }

    private fun localizeArguments(arguments: Array<out Value?>): Array<Any?> {
        val args = arrayListOf<Any?>()
        arguments.forEach {
            if (it != null) {
                args.add(Module.javalize(it, executable!!.context))
            } else {
                args.add(null)
            }
        }
        return args.toArray()
    }
}