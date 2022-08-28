package com.zhufu.opencraft.events;

import com.zhufu.opencraft.data.Info;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class UserLogoutEvent extends Event {
    private final Info info;
    private final boolean showMessage;
    private final HandlerList handlerList = new HandlerList();
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public UserLogoutEvent(@NotNull Info who, boolean showQuitMessage) {
        info = who;
        this.showMessage = showQuitMessage;
    }

    @NotNull
    public Info getInfo() {
        return info;
    }

    public boolean showMessage() {
        return showMessage;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
