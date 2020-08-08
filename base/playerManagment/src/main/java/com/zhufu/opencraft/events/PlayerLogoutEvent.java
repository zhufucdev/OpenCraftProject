package com.zhufu.opencraft.events;

import com.zhufu.opencraft.Info;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerLogoutEvent extends Event {
    private final Info info;
    private final boolean showMessage;
    private final HandlerList handlerList = new HandlerList();
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public PlayerLogoutEvent(@NotNull Info who, @NotNull boolean showQuitMessage) {
        info = who;
        this.showMessage = showQuitMessage;
    }

    @NotNull
    public Info getInfo() {
        return info;
    }

    @NotNull
    public boolean showMessage() {
        return showMessage;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
