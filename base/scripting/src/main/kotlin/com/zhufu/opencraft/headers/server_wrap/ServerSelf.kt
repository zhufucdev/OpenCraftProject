package com.zhufu.opencraft.headers.server_wrap

import java.util.function.Function

class ServerSelf {
    val onServerBoot = arrayListOf<Function<Nothing,Any?>>()
}