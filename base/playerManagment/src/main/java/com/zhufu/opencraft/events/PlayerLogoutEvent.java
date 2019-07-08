package com.zhufu.opencraft.events;

import com.zhufu.opencraft.Info;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerLogoutEvent extends Event {
    private Info info;
    private boolean showMessage;
    private HandlerList handlerList = new HandlerList();
    private static HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList(){
        return handlers;
    }

    public PlayerLogoutEvent(@NotNull Info who, @NotNull boolean showQuitMessage){
        info = who;
        this.showMessage = showQuitMessage;
    }

    @NotNull
    public Info getInfo() {
        return info;
    }

    @NotNull
    public boolean showMesage() {
        return showMessage;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
