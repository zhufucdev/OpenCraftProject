package com.zhufu.opencraft

import org.graalvm.polyglot.Context

interface Header {
    val members: List<Pair<String,Any?>>
}