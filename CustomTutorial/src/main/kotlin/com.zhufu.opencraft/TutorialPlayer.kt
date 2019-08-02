package com.zhufu.opencraft

import org.bukkit.entity.Entity

interface TutorialPlayer {
    var l: (() -> Unit)?
    fun setPastPlayingListener(l: () -> Unit){
        this.l = l
    }

    fun play(entity: Entity){
        l?.invoke()
    }
}