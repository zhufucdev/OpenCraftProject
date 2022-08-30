package com.zhufu.opencraft;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * By default, this sync event is called every two minutes.
 */
public class ServerReloadEvent extends ServerEvent {
    private final static HandlerList handlerList = new HandlerList();

    public ServerReloadEvent() {

    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
