package com.zhufu.opencraft

import java.util.concurrent.Executor
import java.util.concurrent.Executors

class SimpleExecutor : Executor {
    companion object {
        val threadPool = Executors.newCachedThreadPool()
    }
    override fun execute(command: Runnable) {
        threadPool.execute(command)
    }
}