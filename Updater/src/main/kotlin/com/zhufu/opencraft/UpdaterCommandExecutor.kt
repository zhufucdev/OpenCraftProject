package com.zhufu.opencraft

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import java.time.Duration

object UpdaterCommandExecutor : CommandExecutor {
    const val DEFAULT_KICK_MSG = "服务器正在更新"
    var issuedReload: Boolean = false
        private set

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage(Component.text("你无权使用此指令").color(NamedTextColor.RED))
            return true
        }
        val times = Times.times(
            Duration.ofMillis(150),
            Duration.ofDays(2),
            Duration.ZERO
        )
        val title =
            if (args.isNotEmpty()) {
                val combination = args.joinToString(separator = " ").split(';')
                val title = combination.first()
                val subtitle = if (combination.size == 1) "" else combination.drop(1).joinToString(";")
                Title.title(
                    Component.text(title).color(NamedTextColor.AQUA),
                    Component.text(subtitle).color(NamedTextColor.YELLOW),
                    times
                )
            } else
                Title.title(Component.text(DEFAULT_KICK_MSG).color(NamedTextColor.AQUA), Component.empty(), times)
        kickAll(title)
        issuedReload = true
        Bukkit.reload()
        return true
    }

    private fun kickAll(title: Title) {
        Bukkit.getOnlinePlayers().forEach {
            it.showTitle(title)
            it.kick(title.title())
        }
    }
}