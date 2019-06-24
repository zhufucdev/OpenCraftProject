package com.zhufu.opencraft

import org.bukkit.entity.Player

interface AbstractCommandExecutor {
    fun onCommand(player: Player,command: String,step: TutorialManager.Tutorial.TutorialStep){
        val r = ArrayList<String>()
        var sb = StringBuilder()
        command.forEach {
            if (it != ' ')
                sb.append(it)
            else if (sb.isNotEmpty()){
                r.add(sb.toString())
                sb = StringBuilder()
            }
        }
        r.add(sb.toString())
        execute(player,r).invoke(step)
    }
    fun execute(player: Player, command: List<String>): (TutorialManager.Tutorial.TutorialStep) -> Unit
}