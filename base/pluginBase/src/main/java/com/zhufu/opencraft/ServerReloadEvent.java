package com.zhufu.opencraft;

import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerEvent;

/**
 * By default, this sync event is called every two minutes.
 */
public class ServerReloadEvent extends ServerEvent {
    private static HandlerList handlerList = new HandlerList();
    public ServerReloadEvent(){

    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList(){
        return handlerList;
    }
}
