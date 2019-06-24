package com.zhufu.opencraft.events;

import com.zhufu.opencraft.Info;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerLogoutEvent extends Event {
    private Info info;
    private HandlerList handlerList = new HandlerList();
    private static HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList(){
        return handlers;
    }

    public PlayerLogoutEvent(@NotNull Info who){
        info = who;
    }

    @NotNull
    public Info getInfo() {
        return info;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
