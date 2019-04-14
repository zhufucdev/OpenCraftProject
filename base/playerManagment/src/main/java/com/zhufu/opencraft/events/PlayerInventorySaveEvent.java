package com.zhufu.opencraft.events;

import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerEvent;

public class PlayerInventorySaveEvent extends ServerEvent {
    private static HandlerList handlerList = new HandlerList();
    public PlayerInventorySaveEvent(){

    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList(){
        return handlerList;
    }
}
