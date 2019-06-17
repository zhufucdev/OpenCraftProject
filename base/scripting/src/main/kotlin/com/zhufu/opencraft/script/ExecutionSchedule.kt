package com.zhufu.opencraft.script

class ExecutionSchedule {
    private val tasks = arrayListOf<() -> Unit>()
    fun task(runnable: () -> Unit) = tasks.add(runnable)
    fun runAll() {
        val size = tasks.size
        for (i in 0 until size) {
            tasks[0].invoke()
            tasks.removeAt(0)
        }
        Thread.sleep(500)
        if (tasks.isNotEmpty())
            runAll()
    }
}