package com.zhufu.opencraft.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class UserLoginEvent extends PlayerEvent {
    private final static HandlerList handlers = new HandlerList();

    public UserLoginEvent(Player who) {
        super(who);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}
