package com.zhufu.opencraft

import java.io.File
import java.net.InetAddress
import java.util.*

abstract class WebInfo(createNew: Boolean,uuid: UUID? = null) : ServerPlayer(createNew,uuid) {
    abstract val face: File
}