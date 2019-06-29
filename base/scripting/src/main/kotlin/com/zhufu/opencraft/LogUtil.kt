package com.zhufu.opencraft

import org.bukkit.Bukkit
import java.io.File
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.logging.Level

class ServerLogger(val player: ServerPlayer, val level: Level) {
    private val mLog = StringBuilder()
    val log by lazy { mLog.toString() }
    val timeBegin = System.currentTimeMillis()
    var timeEnd = timeBegin
        private set

    fun stream() = LogStream(this)

    fun append(log: String) {
        timeEnd = System.currentTimeMillis()
        mLog.append(log)
        if (player is ChatInfo) {
            player.playerOutputStream.send(
                when (level) {
                    Level.INFO -> TextUtil.info(log)
                    Level.WARNING -> TextUtil.warn(log)
                    Level.SEVERE -> TextUtil.error(log)
                    Level.ALL -> log
                    Level.CONFIG -> log
                    Level.OFF -> log
                    else -> TextUtil.success(log)
                }
            )
            Bukkit.getLogger().info("Sent log to player ${player.name}")
        } else {
            with(Bukkit.getLogger()) {
                log(level, "Log to player ${player.name}:")
                log(level, log)
            }
        }
    }

    fun save(name: String) {
        val date = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(timeEnd)
        with(File(player.playerDir, "$date->$name.log")) {
            if (!parentFile.exists())
                parentFile.mkdirs()
            if (createNewFile())
                writeText(log)
            else if (player is ChatInfo)
                player.playerOutputStream.send(player.lang()["scripting.error.log"].toErrorMessage())
        }
    }

    override fun equals(other: Any?): Boolean =
        other is ServerLogger && other.player == this.player && other.timeBegin == this.timeBegin && other.timeEnd == this.timeEnd

    override fun hashCode(): Int {
        var result = level.hashCode()
        result = 31 * result + timeBegin.hashCode()
        result = 31 * result + timeEnd.hashCode()
        return result
    }
}

class LogStream(private val logger: ServerLogger) : OutputStream() {
    override fun write(b: Int) {
        logger.append(b.toChar().toString())
    }

    override fun write(b: ByteArray?) {
        logger.append(b?.toString(Charset.defaultCharset()) ?: return)
    }
}

fun ServerPlayer.newLogger(level: Level): ServerLogger = ServerLogger(this, level)