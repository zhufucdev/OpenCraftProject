package com.zhufu.opencraft

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class PlayerManagerPlugin : JavaPlugin() {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.isOp){
            sender.sendMessage(TextUtil.error("您没有权限使用此命令"))
            return true
        }
        if (args.isEmpty()){
            sender.sendMessage(TextUtil.error("用法错误"))
            return false
        }
        val expr = buildString {
            args.forEach {
                this.append("$it ")
            }
            this.deleteCharAt(this.lastIndex)
        }
        val expression = Expression(expr)
        fun check(): Boolean{
            sender.sendMessage(TextUtil.error(expression.check()?:return false))
            return true
        }
        if (check()) return true
        fun apply() {
            sender.sendMessage(expression.apply()?:return)
        }
        apply()
        return true
    }
}