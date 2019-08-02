package com.zhufu.opencraft.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerQuitGameEvent extends PlayerEvent {
    private static final HandlerList handlerList = new HandlerList();
    private int which;

    public PlayerQuitGameEvent(Player who, int which){
        super(who);
        this.which = which;
    }

    public int getWhich() {
        return which;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList(){
        return handlerList;
    }
}
