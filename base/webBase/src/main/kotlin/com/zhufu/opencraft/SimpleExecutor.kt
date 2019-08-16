package com.zhufu.opencraft

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SimpleExecutor : Executor {
    companion object {
        val threadPool = Executors.newCachedThreadPool()
    }
    override fun execute(command: Runnable) {
        threadPool.submit(command)[2, TimeUnit.MINUTES]
    }
}