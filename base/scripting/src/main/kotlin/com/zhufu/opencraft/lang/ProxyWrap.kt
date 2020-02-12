package com.zhufu.opencraft.lang

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.proxy.Proxy

interface ProxyWrap: Proxy {
    fun rewrap(newWrapper: Context): ProxyWrap
}