package com.zhufu.opencraft.headers.util

import com.zhufu.opencraft.Base
import com.zhufu.opencraft.Info
import com.zhufu.opencraft.Scripting.loopExecutions
import com.zhufu.opencraft.exceptions.LoopExecutionOutOfBoundError
import com.zhufu.opencraft.lang
import org.bukkit.Bukkit
import java.util.*
import java.util.function.Function
import kotlin.concurrent.fixedRateTimer

object Utils {
    fun getJavaType(i: Any?) = (i?.javaClass?.name) ?: "null"
    fun runSync(f: Function<Array<Any?>, Any?>) {
        Bukkit.getScheduler().runTask(Base.pluginCore) { _ ->
            f.apply(arrayOf(null))
        }
    }

    fun loopBump(id: Int, uuid: String) {
        val times = (loopExecutions[id] ?: 0) + 1
        val info = Info.findByPlayer(UUID.fromString(uuid)) ?: throw IllegalStateException()
        if (times > info.maxLoopExecution){
            Bukkit.getLogger().warning("${info.name} was thrown loop execution out of bound error for more than ${info.maxLoopExecution} call.")
            throw LoopExecutionOutOfBoundError(info.lang()["scripting.error.executionOutOfBound",info.maxLoopExecution])
        }
        loopExecutions[id] = times
    }

    fun runTaskTimer(f: Function<Array<Any?>, Any?>, duration: Long, period: Long) {
        if (duration > 5000) {
            throw IllegalArgumentException("The duration is too long!(maximum is 5000)")
        } else if (duration < 0) {
            throw IllegalArgumentException("The duration must be meaningful!")
        }
        val timeStop = System.currentTimeMillis() + duration
        val originThread = Thread.currentThread()
        val handler = originThread.uncaughtExceptionHandler
        Bukkit.getScheduler().runTaskAsynchronously(Base.pluginCore) { _ ->
            fixedRateTimer(period = period) {
                try {
                    if (timeStop <= System.currentTimeMillis()) {
                        cancel()
                        return@fixedRateTimer
                    }
                    f.apply(arrayOf())
                } catch (e: Throwable) {
                    handler.uncaughtException(originThread, e)
                }
            }
        }
    }

    fun runTaskTimer(f: Function<Array<Any?>, Any?>, duration: Long) = runTaskTimer(f, duration, 20)
}