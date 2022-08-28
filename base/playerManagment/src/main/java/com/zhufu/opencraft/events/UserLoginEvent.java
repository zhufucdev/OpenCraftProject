package com.zhufu.opencraft.events;

import com.zhufu.opencraft.data.Info;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class UserLoginEvent extends PlayerEvent {
    private final static HandlerList handlers = new HandlerList();

    private final Info info;

    public UserLoginEvent(Player who, Info info) {
        super(who);
        this.info = info;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList(){
        return handlers;
    }

    public @NotNull Info getInfo() {
        return info;
    }
}
