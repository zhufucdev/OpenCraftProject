package com.zhufu.opencraft.events;

import com.zhufu.opencraft.Info;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerLoginEvent extends PlayerEvent {
    private final static HandlerList handlers = new HandlerList();

    public PlayerLoginEvent(Player who) {
        super(who);

    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}
