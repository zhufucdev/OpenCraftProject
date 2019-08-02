package com.zhufu.opencraft.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

public class PlayerTeleportedEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private Location from,to;
    private Boolean isCancelled = false;

    public Location getFrom() {
        return from;
    }

    @Nullable
    public Location getTo() {
        return to;
    }

    public PlayerTeleportedEvent(Player who, Location from, Location to) {
        super(who);
        this.from = from;
        this.to = to;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        isCancelled = b;
    }
}
