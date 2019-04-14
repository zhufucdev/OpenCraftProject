package com.zhufu.opencraft;

import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerEvent;

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
