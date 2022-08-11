package com.zhufu.opencraft.api

object ServerCaller {
    private val map = HashMap<String,(List<Any?>) -> Any>()
    operator fun <T: Any> set(name: String, lambda: ((List<Any?>) -> T)?){
        if (lambda != null)
            map[name] = lambda
        else
            map.remove(name)
    }
    operator fun get(name: String) = map[name]
}