package com.zhufu.opencraft;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class CoreInitializedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    private final JavaPlugin instance;

    public CoreInitializedEvent(JavaPlugin instance) {
        this.instance = instance;
    }

    public JavaPlugin getInstance() {
        return instance;
    }
}
